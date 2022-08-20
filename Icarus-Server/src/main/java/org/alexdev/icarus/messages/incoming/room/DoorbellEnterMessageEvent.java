package org.alexdev.icarus.messages.incoming.room;

import org.alexdev.icarus.game.player.Player;
import org.alexdev.icarus.game.room.Room;
import org.alexdev.icarus.game.room.RoomManager;
import org.alexdev.icarus.game.util.RoomUtil;
import org.alexdev.icarus.messages.types.MessageEvent;
import org.alexdev.icarus.server.api.messages.ClientMessage;

public class DoorbellEnterMessageEvent implements MessageEvent {

    @Override
    public void handle(Player player, ClientMessage request) {
        
        int roomId = request.readInt();
        
        Room room = RoomManager.getInstance().getByRoomId(roomId);
        
        if (room == null) {
            return;
        }
        
        RoomUtil.playerRoomEntry(player, room, "");
    }
}
