package core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class ByteBufferOutputStream extends OutputStream {

	private final int DEFAULT_BUFFER_SIZE = 10 * 1024;
	//private final int DEFAULT_COMPACT_SIZE = 1 * 1024;
	
	private ByteBuffer buffer;
	
	public ByteBufferOutputStream() {
		this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
	}
	
	@Override
	public void write(int b) throws IOException {
		this.buffer.put((byte)(b & 0xFF));
	}
	
	/**
	 * @return an estimate of the number of bytes in the stream.
	 */
	public int available() {
		return this.buffer.position();
	}
	
	public void flip() {
		this.buffer.flip();
	}
	
	public void clear() {
		this.buffer.clear();
	}
	
	public ByteBuffer getBuffer() {
		return this.buffer;
	}
}
