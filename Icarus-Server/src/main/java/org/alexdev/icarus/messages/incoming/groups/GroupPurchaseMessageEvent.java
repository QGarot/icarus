package org.alexdev.icarus.messages.incoming.groups;

import java.util.ArrayList;
import java.util.List;

import org.alexdev.icarus.dao.mysql.groups.GroupDao;
import org.alexdev.icarus.game.groups.Group;
import org.alexdev.icarus.game.player.Player;
import org.alexdev.icarus.game.room.Room;
import org.alexdev.icarus.game.room.RoomManager;
import org.alexdev.icarus.game.room.enums.RoomAction;
import org.alexdev.icarus.game.util.BadgeUtil;
import org.alexdev.icarus.messages.outgoing.catalogue.PurchaseNotificationMessageComposer;
import org.alexdev.icarus.messages.outgoing.groups.GroupBadgesMessageComposer;
import org.alexdev.icarus.messages.outgoing.groups.NewGroupInfoMessageComposer;
import org.alexdev.icarus.messages.types.MessageEvent;
import org.alexdev.icarus.server.api.messages.ClientMessage;
import org.alexdev.icarus.util.Util;

public class GroupPurchaseMessageEvent implements MessageEvent {

    @Override
    public void handle(Player player, ClientMessage reader) {
        
        String name = reader.readString();
        String desc = reader.readString();

        int roomId = reader.readInt();
        
        Room room = RoomManager.getInstance().getByRoomId(roomId);
        
        if (!room.hasOwnership(player.getEntityId())) {
            return;
        }
        
        if (room.getGroup() != null) {
            return;
        }
        
        int colourA = reader.readInt();
        int colourB = reader.readInt();
        
        reader.readInt();
        
        int groupBase = reader.readInt();
        int groupBaseColour = reader.readInt();
        int groupItemsLength = reader.readInt() * 3;

        List<Integer> groupItems = new ArrayList<>();

        for (int i = 0; i < (groupItemsLength); i++) {
            groupItems.add(reader.readInt());
        }

        String badge = BadgeUtil.generate(groupBase, groupBaseColour, groupItems);
        Group group = GroupDao.createGroup(name, desc, badge, player.getEntityId(), roomId, Util.getCurrentTimeSeconds(), colourA, colourB);
        
        room.getData().setGroupId(group.getId());
        room.loadGroup();
        room.save();
        
        player.send(new PurchaseNotificationMessageComposer());

        if (player.getRoomUser().getRoomId() != room.getData().getId()) {
            player.getMetadata().set("showGroupHomeroomDialog", true);
            player.performRoomAction(RoomAction.FORWARD_ROOM, roomId);
            
        } else {
            player.send(new NewGroupInfoMessageComposer(roomId, group.getId()));
            room.send(new GroupBadgesMessageComposer(group.getId(), badge));
        }
    }
}
