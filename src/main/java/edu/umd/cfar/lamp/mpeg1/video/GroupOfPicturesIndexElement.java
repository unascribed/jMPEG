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

/** Represents one picture (frame) within a <code>GroupOfPicturesIndex</code>. */
public class GroupOfPicturesIndexElement implements Comparable<GroupOfPicturesIndexElement> {
	private long startPosition;
	private long dataSize;
	private byte type;
	private int displayOrder;

	public GroupOfPicturesIndexElement(long startPosition, long dataSize, byte type, int displayOrder) {
		this.startPosition = startPosition;
		this.dataSize = dataSize;
		this.type = type;
		this.displayOrder = displayOrder;
	}

	public long getStartPosition() {
		return startPosition;
	}

	public long getDataSize() {
		return dataSize;
	}

	public byte getType() {
		return type;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public char getTypeChar() {
		return PictureCodingTypes.getChar(type);
	}

	@Override
	public int compareTo(GroupOfPicturesIndexElement o) {
		if (this.equals(o))
			return 0;

		if (getDisplayOrder() < o.getDisplayOrder())
			return -1;

		return 1;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof GroupOfPicturesIndexElement)
			return ((GroupOfPicturesIndexElement) o).getDisplayOrder() == getDisplayOrder();
		else
			return false;
	}

	@Override
	public int hashCode() {
		return 31 * getDisplayOrder();
	}

	@Override
	public String toString() {
		return "(startPosition: " + startPosition + ", " + "dataSize: " + dataSize + ", " + "type: " + getTypeChar() + ", " + "displayOrder: " + displayOrder + ")";

	}
}
