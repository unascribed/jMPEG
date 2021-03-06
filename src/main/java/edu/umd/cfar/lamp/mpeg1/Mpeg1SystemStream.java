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

package edu.umd.cfar.lamp.mpeg1;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import javax.swing.ProgressMonitorInputStream;

import edu.columbia.ee.flavor.Bitstream;
import edu.umd.cfar.lamp.mpeg1.system.IndexerState;
import edu.umd.cfar.lamp.mpeg1.system.StreamNotFoundException;
import edu.umd.cfar.lamp.mpeg1.system.SystemIndex;
import edu.umd.cfar.lamp.mpeg1.system.SystemStream;

public class Mpeg1SystemStream extends InputStream {
	private final File file ;
	private final FileChannel channel;
	private ByteBuffer buffer = null;
	private SystemIndex systemIndex = null;

	private int currentStreamID = 0;
	private long streamPointer = 0;

	public Mpeg1SystemStream(File file) throws IOException {
		this.file = file;
		this.channel = new RandomAccessFile(file, "r").getChannel();
		long fsize = channel.size();
		this.buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fsize);
	}

	public Mpeg1SystemStream(File file, SystemIndex systemIndex)
			throws IOException {
		this(file);
		setSystemIndex(systemIndex);
	}

	public void writeIndex(File file) throws IOException, MpegException {
		OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		writeIndex(out);
		out.close();
	}

	public void writeIndex(OutputStream out)
			throws IOException, MpegException {
		index();
		systemIndex.writeIndex(new DataOutputStream(out));
	}

	public void writeIndex(
			Component parentComponent,
			Object message,
			File file)
			throws IOException, MpegException {
		index(parentComponent, message);
		writeIndex(file);
	}

	public void writeIndex(
			Component parentComponent,
			Object message,
			OutputStream out)
			throws IOException, MpegException {
		index(parentComponent, message);
		writeIndex(out);
	}

	public void readIndex(File file) throws IOException, MpegException {
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		readIndex(in);
		in.close();
	}

	public void readIndex(InputStream in) throws IOException, MpegException {
		systemIndex = new SystemIndex();
		systemIndex.readIndex(new DataInputStream(in));
	}

	public Mpeg1SystemStream copyStream() throws IOException {
		return new Mpeg1SystemStream(file, systemIndex);
	}

	public void setSystemIndex(SystemIndex systemIndex) {
		this.systemIndex = systemIndex;
	}

	public void setStream(int stream_id) throws IOException, MpegException {
		index();
		systemIndex.getPosition(stream_id, 0);
		currentStreamID = stream_id;
		streamPointer = 0;
	}

	public int getStreamID() {
		return currentStreamID;
	}

	public List<Integer> getStreamList() throws IOException {
		index();
		return systemIndex.getStreamList();
	}

	public List<Integer> getVideoStreamList() throws IOException {
		index();
		return systemIndex.getVideoStreamList();
	}

	public File getFile() {
		return file;
	}

	public SystemIndex getSystemIndex() {
		return systemIndex;
	}

	public void index() throws IOException {
		if (!indexed()) {
			systemIndex = new SystemIndex();
			new SystemStream().index(
					new Bitstream(
							new BufferedInputStream(new FileInputStream(file))),
					new IndexerState(),
					systemIndex);
		}
	}

	public void index(Component parentComponent, Object message)
			throws IOException {
		if (!indexed()) {
			systemIndex = new SystemIndex();
			new SystemStream().index(
					new Bitstream(
							new BufferedInputStream(
									new ProgressMonitorInputStream(
											parentComponent,
											message,
											new FileInputStream(file)))),
					new IndexerState(),
					systemIndex);
		}
	}

	public boolean indexed() {
		return (systemIndex != null);
	}

	public void seek(long bytePosition) throws IOException, MpegException {
		seek(bytePosition, currentStreamID);
	}

	public void seek(long bytePosition, int stream_id)
			throws IOException, MpegException {
		index();
		setStream(stream_id);
		streamPointer = bytePosition;
	}

	// === InputStream ==========================================================================
	@Override
	public int read() throws IOException {
		try {
			long to = systemIndex.getPosition(currentStreamID, streamPointer);
			seekRafile(to);
			streamPointer++;
			return readRafile();
		} catch (StreamNotFoundException snfe) {
			throw new IOException(snfe.toString());
		}
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		if (buffer.length == 0)
			return 0;

		int max = buffer.length;

		long position = 0;
		long lastByte = -1;
		int bytesToRead = 0;

		try {
			position = systemIndex.getPosition(currentStreamID, streamPointer);
			lastByte = systemIndex.getLastByteInPacket(currentStreamID, streamPointer);
			bytesToRead = (int) (lastByte - position) + 1;
		} catch (StreamNotFoundException snfe) {
			throw new IOException(snfe.toString());
		}

		if (position == -1)
			return -1;

		if (bytesToRead > max)
			bytesToRead = max;

		seekRafile(position);
		int result = readRafile(buffer, 0, bytesToRead);
		if (result == -1)
			return -1;
		streamPointer += result;
		return result;
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		channel.close();
	}

	private void seekRafile(long pos) throws IOException {
		((Buffer) this.buffer).position((int) pos);
	}

	private int readRafile() throws IOException {
		return this.buffer.get();
	}

	private int readRafile(byte[] dst, int off, int len) throws IOException {
		try {
			this.buffer.get(dst, off, len);
			return len;
		} catch (BufferUnderflowException bux) {
			return -1;
		}
	}
	// === InputStream ==========================================================================
}
