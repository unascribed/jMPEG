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

package edu.umd.cfar.lamp.mpeg1.system;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import edu.umd.cfar.lamp.mpeg1.IndexException;
import edu.umd.cfar.lamp.mpeg1.MpegException;

public class SystemIndex {
	public static final int MAGIC_NUMBER = 0x11172101; // for ISO/IEC 11172-1 version 1

	/** Contains a list of indices, one for each stream. Maps <code>stream_id</code>s to <code>Vector</code>s of <code>SystemIndexElement</code>s. */
	private final Map<Integer, List<SystemIndexElement>> streamList = new HashMap<>();

	/** Logger for errors in the format and other problems while decoding. */
	private static Logger logger = Logger.getLogger("edu.umd.cfar.lamp.mpeg1.system");

	public void writeIndex(DataOutput out) throws IOException {
		out.writeInt(MAGIC_NUMBER);

		List<Integer> videoStreamList = getVideoStreamList();

		int numStreams = videoStreamList.size();
		out.writeInt(numStreams);

		for (int i = 0; i < numStreams; i++) {
			Integer streamID = videoStreamList.get(i);
			out.writeByte(streamID.intValue());

			List<SystemIndexElement> packetList = streamList.get(streamID);
			int numPackets = packetList.size();
			out.writeInt(numPackets);

			for (int j = 0; j < numPackets; j++) {
				SystemIndexElement sie = packetList.get(j);
				sie.writeIndex(out);
			}
		}
	}

	public void readIndex(DataInput in) throws IOException, MpegException {
		int magicNumber = in.readInt();
		byte version = (byte) (magicNumber & 0xFF); // version is last byte of magic number

		if ((magicNumber >>> 8) != (MAGIC_NUMBER >>> 8)) // compare first 3 bytes of magic number
			throw new IndexException("Invalid magic number: " + Integer.toHexString(magicNumber));

		switch (version) {
			case 0x01:
				streamList.clear();

				int numStreams = in.readInt();

				for (int i = 0; i < numStreams; i++) {
					int stream_id = in.readUnsignedByte();
					int numPackets = in.readInt();

					int j = 0;
					try {
						for (j = 0; j < numPackets; j++) {
							long systemStreamDataStartPosition = in.readLong();
							int packetDataLength = in.readInt();
							addPacket(stream_id, systemStreamDataStartPosition, packetDataLength);
						}
					} catch (EOFException e) {
						logger.warning(j + " read " + e.getLocalizedMessage());
					}
				}

		}
	}

	@Override
	public String toString() {
		String result = new String();

		for (Map.Entry<Integer, List<SystemIndexElement>> en : streamList.entrySet()) {
			for (SystemIndexElement sie : en.getValue()) {
				result += sie + " ";
			}
			result += "\n";
		}

		return result;
	}

	/** @return a <code>list</code> of <code>Integer</code>s, corresponding to the list of <code>stream_id</code>s. */
	public List<Integer> getStreamList() {
		return new ArrayList<>(streamList.keySet());
	}

	public List<Integer> getVideoStreamList() {
		List<Integer> allStreamsList = getStreamList();
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < allStreamsList.size(); i++) {
			Integer element = allStreamsList.get(i);
			if (StreamIDs.isVideoStream(element.intValue())) {
				result.add(element);
			}
		}
		return result;
	}

	/**
	 * Adds a <code>SystemIndexElement</code> to the index. The
	 * <code>SystemIndexElement</code> is calculated based on the
	 * <code>stream_id</code>, the
	 * <code>systemStreamBytePosition</code>, and the index so far.
	 * 
	 * @param stream_id
	 *            the stream
	 * @param systemStreamBytePosition
	 *            the byte position into the system stream
	 * @param packetDataLength
	 *            the length of the packet
	 */
	public void addPacket(int stream_id, long systemStreamBytePosition, int packetDataLength) {
		Integer streamID = Integer.valueOf(stream_id);
		if (!streamList.containsKey(streamID)) {
			streamList.put(streamID, new ArrayList<SystemIndexElement>());
		}

		long elementaryStreamBytePosition;
		List<SystemIndexElement> li = streamList.get(streamID);
		if (!li.isEmpty()) {
			SystemIndexElement lastSystemIndexElement = li.get(li.size()-1);
			elementaryStreamBytePosition = lastSystemIndexElement.getElementaryStreamDataStartPosition() + lastSystemIndexElement.getPacketDataLength();
		} else {
			elementaryStreamBytePosition = 0;
		}
		streamList.get(streamID).add(new SystemIndexElement(systemStreamBytePosition, elementaryStreamBytePosition, packetDataLength));
	}

	/**
	 * Gets the byte position in the System Stream of the given byte
	 * position in the elementary stream with the given
	 * <code>stream_id</code>.
	 * 
	 * @param stream_id
	 *            the stream
	 * @param bytePosition
	 *            the position into the stream
	 * @return the position in the file
	 * @throws StreamNotFoundException if no stream exists with this ID
	 */
	public long getPosition(int stream_id, long bytePosition) throws StreamNotFoundException {
		Integer streamID = Integer.valueOf(stream_id);

		List<SystemIndexElement> streamIndex = streamList.get(streamID);

		if (streamIndex == null) {
			throw new StreamNotFoundException("stream_id: " + stream_id);
		}

		// binary search
		int low = 0;
		int high = streamIndex.size() - 1;
		while (low <= high) {
			int mid = (low + high) / 2;
			SystemIndexElement sie = streamIndex.get(mid);
			int c = sie.findByte(bytePosition);
			if (c < 0)
				high = mid - 1;
			else if (c > 0)
				low = mid + 1;
			else {
				long systemPosition = sie.getSystemStreamDataStartPosition();
				long elementaryPosition = sie.getElementaryStreamDataStartPosition();
				return (bytePosition - elementaryPosition) + systemPosition;
			}
		}
		return -1;
	}

	public long getLastByteInPacket(int stream_id, long bytePosition) throws StreamNotFoundException {
		Integer streamID = Integer.valueOf(stream_id);

		List<SystemIndexElement> streamIndex = streamList.get(streamID);

		if (streamIndex == null) {
			throw new StreamNotFoundException("stream_id: " + stream_id);
		}

		// binary search
		int low = 0;
		int high = streamIndex.size() - 1;
		while (low <= high) {
			int mid = (low + high) / 2;
			SystemIndexElement sie = streamIndex.get(mid);
			int c = sie.findByte(bytePosition);
			if (c < 0)
				high = mid - 1;
			else if (c > 0)
				low = mid + 1;
			else {
				long systemPosition = sie.getSystemStreamDataStartPosition();
				int numBytes = sie.getPacketDataLength();
				return numBytes + systemPosition - 1;
			}
		}
		return -1;
	}
}
