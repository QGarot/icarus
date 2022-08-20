package org.alexdev.icarus.server.api.messages;

import io.netty.buffer.ByteBuf;

public interface Response {
    
    /**
     * Write string.
     *
     * @param obj the obj
     */
    public void writeString(Object obj);
    
    /**
     * Write int.
     *
     * @param obj the obj
     */
    public void writeInt(Integer obj);
    
    /**
     * Write int.
     *
     * @param obj the obj
     */
    public void writeInt(Boolean obj);
    
    /**
     * Write short.
     *
     * @param obj the obj
     */
    public void writeShort(int obj);
    
    /**
     * Write bool.
     *
     * @param obj the obj
     */
    public void writeBool(Boolean obj);
    
    /**
     * Write object.
     *
     * @param serialise the serialisable object.
     */
    public void writeObject(Serialisable serialise);
    
    /**
     * Gets the body string.
     *
     * @return the body string
     */
    public String getBodyString();
    
    /**
     * Gets the header.
     *
     * @return the header
     */
    public int getHeader();
}
