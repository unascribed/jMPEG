/***************************************
 *            ViPER-MPEG               *
 *  The Video Processing               *
 *         Evaluation Resource         *
 *             MPEG-1 Decoder          *
 * Distributed under the LGPL license  *
 *        Terms available at gnu.org.  *
 *                                     *
 *  Copyright University of Maryland,  *
 *                      College Park.  *
 ***************************************/

package edu.umd.cfar.lamp.mpeg1.video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Indexes within a Group of Pictures. */
public class GroupOfPicturesIndex {
	private List<GroupOfPicturesIndexElement> codingOrderIndex = null;
	private List<GroupOfPicturesIndexElement> displayOrderIndex = null;
	private boolean displayOrderIndexed = false;

	public GroupOfPicturesIndex() {
		codingOrderIndex = new ArrayList<GroupOfPicturesIndexElement>();
	}

	/**
	 * Adds a <code>GroupOfPicturesIndexElement</code>, which
	 * represents one picture (frame) in the Group of Pictures, to
	 * the index.
	 * 
	 * @param startPosition the start position
	 * @param dataSize the data length
	 * @param type the picture type
	 * @param displayOrder the display order
	 */
	public void addPicture(long startPosition, long dataSize, byte type, int displayOrder) {
		codingOrderIndex.add(new GroupOfPicturesIndexElement(startPosition, dataSize, type, displayOrder));
		displayOrderIndexed = false;
	}

	/**
	 * @param n
	 *            the frame
	 * @return the byte position (in the video stream) of the
	 *         (zero-based) <code>n</code>th picture (frame) in this Group
	 *         Of Pictures (in display order).
	 */
	public long getPositionOfPicture(int n) throws FrameNotFoundException {
		try {
			return getElementsInDisplayOrder().get(n).getStartPosition();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new FrameNotFoundException("Picture " + n + " not in " + this);
		}
	}

	public int getLastIOrPPicture(int n) throws FrameNotFoundException {
		List<GroupOfPicturesIndexElement> codingOrder = getElementsInCodingOrder();

		int codingOrderIndex = mapDisplayOrderToCodingOrder(n);
		for (int i = codingOrderIndex - 1; i >= 0; i--) {
			switch (codingOrder.get(i).getType()) {
				case PictureCodingTypes.TYPE_I:
				case PictureCodingTypes.TYPE_P:
					return mapCodingOrderToDisplayOrder(i);
			}
		}

		throw new FrameNotFoundException();
	}

	public int getLastIOrPPicture() throws FrameNotFoundException {
		List<GroupOfPicturesIndexElement> codingOrder = getElementsInCodingOrder();

		for (int i = codingOrder.size() - 1; i >= 0; i--) {
			switch (codingOrder.get(i).getType()) {
				case PictureCodingTypes.TYPE_I:
				case PictureCodingTypes.TYPE_P:
					return mapCodingOrderToDisplayOrder(i);
			}
		}

		throw new FrameNotFoundException();
	}

	public long getPositionOfPictureInCodingOrder(int n) throws FrameNotFoundException {
		try {
			return getElementsInCodingOrder().get(n).getStartPosition();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new FrameNotFoundException();
		}
	}

	/** @return a <code>List<code> of <code>GroupOfPicturesIndexElement</code>s in display order. */
	public List<GroupOfPicturesIndexElement> getElementsInDisplayOrder() {
		if (!displayOrderIndexed) {
			displayOrderIndex = new ArrayList<>(codingOrderIndex);
			Collections.sort(displayOrderIndex);
			displayOrderIndexed = true;
		}
		return displayOrderIndex;
	}

	/** @return a <code>List<code> of <code>GroupOfPicturesIndexElement</code>s in coding order. */
	public List<GroupOfPicturesIndexElement> getElementsInCodingOrder() {
		return codingOrderIndex;
	}

	/**
	 * Maps a (0-based) frame in dislay order to a (0-based) frame in coding order.
	 */
	public int mapDisplayOrderToCodingOrder(int displayOrder) throws FrameNotFoundException {
		int i = codingOrderIndex.indexOf(getElementsInDisplayOrder().get(displayOrder));
		if (i == -1)
			throw new FrameNotFoundException();
		return i;
	}

	/**
	 * Maps a (0-based) frame in coding order to a (0-based) frame in display order.
	 */
	public int mapCodingOrderToDisplayOrder(int codingOrder) throws FrameNotFoundException {
		int i = getElementsInDisplayOrder().indexOf(codingOrderIndex.get(codingOrder));
		if (i == -1)
			throw new FrameNotFoundException();
		return i;
	}

	/**
	 * Returns the type of the (0-based) frame in coding order.
	 */
	public byte getTypeOfPictureInCodingOrder(int n) throws FrameNotFoundException {
		return codingOrderIndex.get(n).getType();
	}

	public byte getTypeOfPicture(int n) throws FrameNotFoundException {
		return getElementsInDisplayOrder().get(n).getType();
	}

	@Override
	public String toString() {
		return "(" + codingOrderIndex + ")";
	}
}
