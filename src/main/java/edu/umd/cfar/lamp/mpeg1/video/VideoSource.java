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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

import edu.umd.cfar.lamp.mpeg1.Mpeg1SystemStream;
import edu.umd.cfar.lamp.mpeg1.MpegException;

public class VideoSource extends InputStream {
	public static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

	private static Logger logger = Logger.getLogger("edu.umd.cfar.lamp.mpeg1.video");

	private File file = null;
	private RandomAccessFile rafile = null;
	private Mpeg1SystemStream stream = null;

	private byte[] buffer;
	private int currentBufferPosition = 0;
	private int unreadBytesInBuffer = 0;

	public VideoSource(File file) throws IOException {
		this(file, DEFAULT_BUFFER_SIZE);
	}

	public VideoSource(Mpeg1SystemStream stream, int stream_id) throws IOException, MpegException {
		this(stream, stream_id, DEFAULT_BUFFER_SIZE);
	}

	public VideoSource(File file, int bufferSize) throws IOException {
		this.file = file;
		this.rafile = new RandomAccessFile(file, "r");
		this.buffer = new byte[bufferSize];
	}

	public VideoSource(Mpeg1SystemStream stream, int stream_id, int bufferSize) throws IOException, MpegException {
		this.stream = stream;
		this.stream.setStream(stream_id);
		this.buffer = new byte[bufferSize];
	}

	public int getStreamID() {
		if (isSystemStream())
			return stream.getStreamID();
		else
			return 0;
	}

	public VideoSource copySource() throws IOException, MpegException {
		if (isFile())
			return new VideoSource(file, buffer.length);
		if (isSystemStream())
			return new VideoSource(stream.copyStream(), stream.getStreamID(), buffer.length);

		logger.warning("not file or stream");

		return null;
	}

	public boolean isFile() {
		return (file != null);
	}

	public boolean isSystemStream() {
		return (stream != null);
	}

	public void seek(long offset) throws IOException, MpegException {
		flushBuffer();

		if (isFile())
			rafile.seek(offset);
		if (isSystemStream())
			stream.seek(offset);
	}

	public void flushBuffer() {
		unreadBytesInBuffer = 0;
	}

	// === InputStream ==========================================================================
	@Override
	public int available() throws IOException {
		return unreadBytesInBuffer;
	}

	@Override
	public void close() throws IOException {
		if (isFile())
			rafile.close();

		if (isSystemStream())
			stream.close();
	}

	@Override
	public int read() throws IOException {
		if (unreadBytesInBuffer == 0) {
			int bytesRead = 0;

			try {
				if (isFile())
					bytesRead = rafile.read(buffer);
				else if (isSystemStream())
					bytesRead = stream.read(buffer);
				else
					throw new IOException("Cannot fill buffer: no source.");

				if (bytesRead == -1)
					return -1;
			} catch (EOFException eofe) {
				return -1;
			}

			unreadBytesInBuffer = bytesRead;
			currentBufferPosition = 0;
		}

		int result = 0x000000FF & buffer[currentBufferPosition];
		currentBufferPosition++;
		unreadBytesInBuffer--;
		return result;
	}
	// === InputStream ==========================================================================

}
