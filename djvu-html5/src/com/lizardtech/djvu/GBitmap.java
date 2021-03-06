//C- -------------------------------------------------------------------
//C- Java DjVu (r) (v. 0.8)
//C- Copyright (c) 2004-2005 LizardTech, Inc.  All Rights Reserved.
//C- Java DjVu is protected by U.S. Pat. No.C- 6,058,214 and patents
//C- pending.
//C-
//C- This software is subject to, and may be distributed under, the
//C- GNU General Public License, Version 2. The license should have
//C- accompanied the software or you may obtain a copy of the license
//C- from the Free Software Foundation at http://www.fsf.org .
//C-
//C- The computer code originally released by LizardTech under this
//C- license and unmodified by other parties is deemed "the LIZARDTECH
//C- ORIGINAL CODE."  Subject to any third party intellectual property
//C- claims, LizardTech grants recipient a worldwide, royalty-free,
//C- non-exclusive license to make, use, sell, or otherwise dispose of
//C- the LIZARDTECH ORIGINAL CODE or of programs derived from the
//C- LIZARDTECH ORIGINAL CODE in compliance with the terms of the GNU
//C- General Public License.   This grant only confers the right to
//C- infringe patent claims underlying the LIZARDTECH ORIGINAL CODE to
//C- the extent such infringement is reasonably necessary to enable
//C- recipient to make, have made, practice, sell, or otherwise dispose
//C- of the LIZARDTECH ORIGINAL CODE (or portions thereof) and not to
//C- any greater extent that may be necessary to utilize further
//C- modifications or combinations.
//C-
//C- The LIZARDTECH ORIGINAL CODE is provided "AS IS" WITHOUT WARRANTY
//C- OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//C- TO ANY WARRANTY OF NON-INFRINGEMENT, OR ANY IMPLIED WARRANTY OF
//C- MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
//C-
//C- In addition, as a special exception, LizardTech Inc. gives permission
//C- to link the code of this program with the proprietary Java
//C- implementation provided by Sun (or other vendors as well), and
//C- distribute linked combinations including the two. You must obey the
//C- GNU General Public License in all respects for all of the code used
//C- other than the proprietary Java implementation. If you modify this
//C- file, you may extend this exception to your version of the file, but
//C- you are not obligated to do so. If you do not wish to do so, delete
//C- this exception statement from your version.
//C- -------------------------------------------------------------------
//C- Developed by Bill C. Riemers, Foxtrot Technologies Inc. as work for
//C- hire under US copyright laws.
//C- -------------------------------------------------------------------
//
package com.lizardtech.djvu;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.typedarrays.shared.Uint8Array;

/**
 * This class represents bitonal and gray scale pixel images.
 *
 * @author Bill C. Riemers
 * @version $Revision: 1.15 $
 */
public class GBitmap
  extends GMap
{
    
  //~ Instance fields --------------------------------------------------------

	private static final int PIXEL_OFFSET = 3; // alpha offset in image data

  /** Color depth */
  protected int grays = 0;

  /** number of border pixels */
  private int border = 0;

  /** number of bytes in each row */
  private int rowSize = 0;

  /** end of the buffer  */
  private int maxRowOffset = 0;
  
  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new GBitmap object.
   */
  public GBitmap()
  {
      super(BYTES_PER_PIXEL, PIXEL_OFFSET, PIXEL_OFFSET, PIXEL_OFFSET, true);
  }

  //~ Methods ----------------------------------------------------------------

	@Override
	public void putData(Context2d target) {
		target.putImageData(imageData, -border, 0);
	}

	

  /**
   * Query a pixel as boolean
   *
   * @param offset position to query
   *
   * @return true if zero
   */
  public final boolean getBooleanAt(int offset)
  {
    return (offset < border) || (offset >= maxRowOffset)
    || (getByteAt(offset) == 0);
  }

  /**
   * Set the pixel value.
   *
   * @param offset position of the pixel to set
   * @param value gray scale value to set
   */
  public final void setByteAt(
    int offset,
    int value)
  {
    if((offset >= border) || (offset < maxRowOffset))
    {
      data.set(offset * ncolors + PIXEL_OFFSET, 255 * value / (grays - 1));
    }
  }

  /**
   * Query the pixel at a particular location
   *
   * @param offset the pixel location
   *
   * @return the gray scale value
   */
  public final int getByteAt(final int offset)
  {
	int value = offset >= 0 ? data.get(offset * ncolors + PIXEL_OFFSET) : 0;
    return ((value * (grays - 1) + (grays - 2))) / 255;
  }

  /**
   * Query the color depth.
   *
   * @return number of grays
   */
  public final int getGrays()
  {
    return grays;
  }

  /**
   * Set the color depth
   *
   * @param ngrays number of grays
   *
   * @throws IllegalArgumentException if the number of grays is less than 2 or greater than 256
   */
  public void setGrays(int ngrays)
  {
    if((ngrays < 2) || (ngrays > 256))
    {
      throw new IllegalArgumentException(
        "(GBitmap::set_grays) Illegal number of gray levels");
    }

    grays = ngrays;
  }

  /**
   * Set the number of bytes per rows.
   *
   * @param rowSize the number of bytes per row
   */
  private final void setRowSize(final int rowSize)
  {
    this.rowSize   = rowSize;
    maxRowOffset    = rowOffset(nrows);
  }

  /**
   * Set the number of rows.
   *
   * @param rows number of rows
   */
  private final void setRows(final int rows)
  {
    if(rows != nrows)
    {
      nrows          = rows;
      maxRowOffset   = rowOffset(nrows);
    }
  }

  /**
   * Insert another bitmap at the specified location.  Note that both bitmaps
   * need to have the same number of grays.
   *
   * @param bm bitmap to insert
   * @param xh horizontal location to insert at
   * @param yh vertical location to insert at
   * @param subsample rate to subsample at
   *
   * @return true if the blit intersected this bitmap
   */
  public boolean blit(
    final GBitmap bm,
    final int     xh,
    final int     yh,
    final int     subsample)
  {
    int pidx = 0;
    int qidx = 0;

    if(subsample == 1)
    {
      return insertMap(bm, xh, yh, true);
    }

    if(
      (xh >= (ncolumns * subsample))
      || (yh >= (nrows * subsample))
      || ((xh + bm.columns()) < 0)
      || ((yh + bm.rows()) < 0))
    {
      return false;
    }

    if(bm.data != null)
    {
      int dr  = yh / subsample;
      int dr1 = yh - (subsample * dr);

      if(dr1 < 0)
      {
        dr--;
        dr1 += subsample;
      }

      int zdc  = xh / subsample;
      int zdc1 = xh - (subsample * zdc);

      if(zdc1 < 0)
      {
        zdc--;
        zdc1 += subsample;
      }

      int sr  = 0;

      for(; sr < bm.rows(); sr++)
      {
        if((dr >= 0) && (dr < nrows))
        {
          int dc  = zdc;
          int dc1 = zdc1;
          qidx   = bm.rowOffset(sr);
          pidx   = rowOffset(dr);

          for(int sc = 0; sc < bm.columns(); sc++)
          {
            if((dc >= 0) && (dc < ncolumns))
            {
              int i = pidx + dc;
              setByteAt(i, getByteAt(i) + bm.getByteAt(qidx + sc));
            }

            if(++dc1 >= subsample)
            {
              dc1 = 0;
              dc++;
            }
          }
        }

        if(++dr1 >= subsample)
        {
          dr1 = 0;
          dr++;
        }
      }
    }

    return true;
  }

  /**
   * Query the start offset of a row.
   *
   * @param row the row to query
   *
   * @return the offset to the pixel data
   */
  @Override
public final int rowOffset(final int row)
  {
    return (row * rowSize) + border;
  }

  /**
   * Query the number of bytes per row.
   *
   * @return bytes per row
   */
  @Override
public final int getRowSize()
  {
    return rowSize;
  }

  
  /**
   * Insert the reference map at the specified location.
   *
   * @param bit map to insert
   * @param dx horizontal position to insert at
   * @param dy vertical position to insert at
   * @param doBlit true if the gray scale values should be added
   *
   * @return true if pixels are inserted
   */
  private boolean insertMap(
    final GBitmap bit,    
    final int  dx,
    final int  dy,
    final boolean doBlit)
  {
    final int x0 = Math.max(dx, 0);
    int       y0 = Math.max(dy, 0);
    final int x1 = Math.max(-dx, 0);
    int y1 = Math.max(-dy, 0);
    final int w  = Math.min(columns() - x0,  bit.columns() - x1);
    int       h  = Math.min(rows() - y0, bit.rows() - y1);

    if((w > 0) && (h > 0))
    {
      final int gmax=(byte)(grays-1);
      do
      {
        int offset    = rowOffset(y0++) + x0;
        int refOffset = bit.rowOffset(y1++) + x1;

        if(doBlit)
        {
          // This is not really correct.  We should reduce the original level by the
          // amount of the new level.  But since we are normally dealing with non-overlapping
          // or bitonal blits it really doesn't matter.
        	int i         = w;
          do
          {
            final int g=getByteAt(offset) + bit.getByteAt(refOffset++);
            setByteAt(offset++, (byte) Math.min(g, gmax));
          }
          while(--i > 0);
        }
        else
        {
          data.set(bit.data.subarray(refOffset * ncolors, (refOffset + w) * ncolors), offset * ncolors);
        }
      }
      while(--h > 0);
      return true;
    }
    return false;
  }

  /**
   * Initialize this image with the specified values.
   *
   * @param arows number of rows
   * @param acolumns number of columns
   * @param aborder width of the border
   *
   * @return the initialized image map
   */
  public GBitmap init(
    final int arows,
    final int acolumns,
    final int aborder)
  {
    data    = null;
    grays   = 2;
    setRows(arows);
    ncolumns   = acolumns;
    border     = aborder;
    setRowSize(ncolumns + border);

    int npixels = rowOffset(nrows) * ncolors;

    if(npixels > 0)
    {
		setImageData(imageDataFactory.createImageData(rowSize, nrows + 3));
    }

    return this;
  }

  /**
   * Initialize this map by copying a reference map
   *
   * @param ref map to copy
   * @param aborder number of border pixels
   *
   * @return the initialized map
   */
  private GBitmap init(
    final GBitmap ref,
    final int     aborder)
  {
    if(this != ref)
    {
      init(
        ref.rows(),
        ref.columns(),
        aborder);
      grays = ref.grays;

      for(int i = 0; i < nrows; i++)
      {
    	  Uint8Array refRow = ref.data.subarray(ref.rowOffset(i) * ncolors,
    			  ref.rowOffset(i) + ncolumns * ncolors);
    	  data.set(refRow, rowOffset(i) * ncolors);
      }
    }
    else if(aborder > border)
    {
      setMinimumBorder(aborder);
    }

    return this;
  }

  /**
   * Set the minimum border needed
   *
   * @param minimum the minumum border needed
   */
  public void setMinimumBorder(final int minimum)
  {
    if(border < minimum)
    {
      if(data != null)
      {
        GBitmap tmp = new GBitmap().init(this, minimum);
        setRowSize(tmp.getRowSize());
        data       = tmp.data;
        tmp.data   = null;

        if(DjVuOptions.COLLECT_GARBAGE)
        {
          System.gc();
        }
      }

      setBorder(minimum);
    }
  }

  /**
   * Set the border width.
   *
   * @param border border width
   */
  private void setBorder(final int border)
  {
    this.border    = border;
    maxRowOffset   = rowOffset(nrows);
  }

  /**
   * Query if we are allowed to skip the ramp call.
   *
   * @return true if not 24 bit color
   */
  @Override
public boolean isRampNeeded()
  {
    return true;
  }
  
  /**
   * Find the bounding box for non-white pixels.
   *
   * @return bounding rectangle
   */
  public synchronized GRect compute_bounding_box()
  {
    final int w = columns();
    final int h = rows();
    final int s = getRowSize();

    int xmin, xmax, ymin, ymax;
    for(xmax = w - 1; xmax >= 0; xmax--)
    {
      int       p  = rowOffset(0) + xmax;
      final int pe = p + (s * h);

      while((p < pe) && getBooleanAt(p))
      {
        p += s;
      }

      if(p < pe)
      {
        break;
      }
    }

    for(ymax = h - 1; ymax >= 0; ymax--)
    {
      int       p  = rowOffset(ymax);
      final int pe = p + w;
      
      while((p < pe) && getBooleanAt(p))
      {
        ++p;
      }

      if(p < pe)
      {
        break;
      }
    }

    for(xmin = 0; xmin <= xmax; xmin++)
    {
      int       p  = rowOffset(0) + xmin;
      final int pe = p + (s * h);

      while((p < pe) && getBooleanAt(p))
      {
        p += s;
      }

      if(p < pe)
      {
        break;
      }
    }

    for(ymin = 0; ymin <= ymax; ymin++)
    {
      int       p  = rowOffset(ymin);
      final int pe = p + w;
      
      while((p < pe) && getBooleanAt(p))
      {
        ++p;
      }

      if(p < pe)
      {
        break;
      }
    }
    final GRect retval=new GRect();
    retval.xmin=xmin;
    retval.xmax=xmax;
    retval.ymin=ymin;
    retval.ymax=ymax;
    return retval;
  }
}
