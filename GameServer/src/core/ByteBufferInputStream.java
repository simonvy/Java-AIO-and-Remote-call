package core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class ByteBufferInputStream extends InputStream {
	
	private final int DEFAULT_BUFFER_SIZE = 10 * 1024;
	private final int DEFAULT_COMPACT_SIZE = 1 * 1024;
	
	private ByteBuffer buffer;
	
	private int position;
	private int mark;
	
	public ByteBufferInputStream() {
		this.buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
		this.position = 0;
		this.mark = -1;
	}
	
	@Override
	public int read() throws IOException {
		if (available() > 0) {
			int retval = (int)this.buffer.get(position) & 0xFF;
			position ++;
			return retval;
		}
		return -1;
	}
	
	@Override
	public int read(byte[] data, int offset, int len) throws IOException {
		if (available() > 0) {
			len = Math.min(Math.min(len, available()), data.length - offset);
			len = Math.max(len, 0);
			if (len > 0) {
				System.arraycopy(buffer.array(), position, data, offset, len);
				position += len;
			}
			return len;
		}
		return -1;
	}
	
	@Override
	public int available() {
		return this.buffer.position() - position;
	}
	
	public void compact() {
		if (this.buffer.remaining() < DEFAULT_COMPACT_SIZE) {
			int available = this.available();
			
			this.buffer.limit(this.buffer.position());
			this.buffer.position(this.position);
			this.buffer.compact();
			this.buffer.position(available);
			
			this.position = 0;
			this.mark = -1;
		}
	}
	
	public ByteBuffer getBuffer() {
		return this.buffer;
	}
	
	@Override
	public boolean markSupported() {
		return true;
	}
	
	@Override
	public void mark(int reserved) {
		this.mark = this.position;
	}
	
	@Override
	public void reset() {
		this.position = this.mark;
		this.mark = -1;
	}
	
	public void clear() {
		this.mark = -1;
		this.position = 0;
		this.buffer.clear();
	}
}
