package org.alexdev.icarus.messages.incoming.room.user;

import org.alexdev.icarus.game.player.Player;
import org.alexdev.icarus.game.room.Room;
import org.alexdev.icarus.game.room.user.RoomUser;
import org.alexdev.icarus.messages.types.MessageEvent;
import org.alexdev.icarus.server.api.messages.ClientMessage;

public class DropItemMessageEvent implements MessageEvent {

    @Override
    public void handle(Player player, ClientMessage reader) {
        
        RoomUser roomUser = player.getRoomUser();

        if (roomUser == null) {
            return;
        }

        Room room = roomUser.getRoom();

        if (room == null) {
            return;
        }
        
        roomUser.carryItem(0);
    }
}
