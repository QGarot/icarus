package org.alexdev.icarus.game.room.user;

import org.alexdev.icarus.dao.mysql.room.RoomDao;
import org.alexdev.icarus.game.commands.CommandManager;
import org.alexdev.icarus.game.entity.Entity;
import org.alexdev.icarus.game.entity.EntityStatus;
import org.alexdev.icarus.game.entity.EntityType;
import org.alexdev.icarus.game.item.Item;
import org.alexdev.icarus.game.item.interactions.Interaction;
import org.alexdev.icarus.game.item.interactions.InteractionType;
import org.alexdev.icarus.game.messenger.InstantMessage;
import org.alexdev.icarus.game.pathfinder.Pathfinder;
import org.alexdev.icarus.game.pathfinder.Position;
import org.alexdev.icarus.game.player.Player;
import org.alexdev.icarus.game.plugins.PluginEvent;
import org.alexdev.icarus.game.plugins.PluginManager;
import org.alexdev.icarus.game.room.Room;
import org.alexdev.icarus.game.room.model.RoomTile;
import org.alexdev.icarus.game.room.model.Rotation;
import org.alexdev.icarus.messages.outgoing.room.user.CarryObjectMessageComposer;
import org.alexdev.icarus.messages.outgoing.room.user.DanceMessageComposer;
import org.alexdev.icarus.messages.outgoing.room.user.TalkMessageComposer;
import org.alexdev.icarus.messages.outgoing.user.effects.DisplayEffectMessageComposer;
import org.alexdev.icarus.messages.types.MessageComposer;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomUser {

    private int virtualId;
    private int chatColor;

    private int danceId;
    private int effectId;

    private int roomRequestedId;
    private int chatCount;
    private int lookResetTime;
    private int teleportRoomId;
    private int carryItem;

    private long chatFloodTimer;

    private boolean isWalking;
    private boolean needsUpdate;
    private boolean isTeleporting;
    private boolean isWalkingAllowed;

    private Entity entity;
    private Room room;

    private Item currentItem;

    private Position position;
    private Position goal;
    private Position nextPosition;

    private AtomicInteger carryTimer;

    private HashMap<EntityStatus, String> statuses;
    private LinkedList<Position> path;

    public RoomUser(Entity entity) {
        this.clearUserData();
        this.entity = entity;
    }
    /**
     * Refresh the status of the user by checking if
     * they're sitting on items, or not..s
     */
    public void refreshItemStatus() {

        boolean allowUpdate = false;

        Item previousItem = this.currentItem;
        double previousHeight = this.position.getZ();

        Item item = this.room.getMapping().getHighestItem(this.position.getX(), this.position.getY());

        if (item != null && item.isWalkable()) {
            this.currentItem = item;
        } else {
            this.currentItem = null;
        }
        // Should we apply a height update?
        if (item != null && Position.getHeightDifference(previousHeight, item.getTotalHeight()) >= 0.1) {
            allowUpdate = true;
        }

        /**
         * If we removed an item from underneath the player OR
         * the current item doesn't allow to sit or lay but the previous item did.
         */
        if (this.currentItem == null || (!this.currentItem.getDefinition().allowSitOrLay() && previousItem != null && previousItem.getDefinition().allowSitOrLay())) {

            if (this.containsStatus(EntityStatus.LAY)) {
                this.removeStatus(EntityStatus.LAY);
            }

            if (this.containsStatus(EntityStatus.SIT)) {
                this.removeStatus(EntityStatus.SIT);
            }

            allowUpdate = true;

        } else {

            Interaction handler = this.currentItem.getDefinition().getInteractionType().get();

            if (handler != null) {
                handler.onStopWalking(this.currentItem, this);

                if (handler.allowStopWalkingUpdate(this.currentItem)) {
                    allowUpdate = true;
                }
            }
        }

        if (allowUpdate) {
            this.updateNewHeight(this.position);
            this.needsUpdate = true;
        }
    }

    /**
     * Contains status.
     *
     * @param status the status
     * @return true, if successful
     */
    public boolean containsStatus(EntityStatus status) {
        return this.statuses.containsKey(status);
    }

    /**
     * Removes the status.
     *
     * @param status the status
     */
    public void removeStatus(EntityStatus status) {
        this.statuses.remove(status);
    }

    /**
     * Sets the status.
     *
     * @param status the status
     * @param value the value
     */
    public void setStatus(EntityStatus status, String value) {

        if (this.containsStatus(status)) {
            this.removeStatus(status);
        }

        this.statuses.put(status, value);
    }

    /**
     * Chat without spam checking, only sends to self.
     *
     * @param type the type
     * @param message - the message to chat to room
     */
    public void chatSelf(ChatType type, String message) {
        if (this.entity.getType() == EntityType.PLAYER) {
            ((Player)entity).send(new TalkMessageComposer(this, type, message, this.chatColor));
        }
    }

    /**
     * Chat and broadcast message in room, supports spam checking.
     *
     * @param message the message
     * @param type the type
     */
    public void chat(String message, ChatType type) {

        //layer.send(new FloodFilterMessageComposer(GameSettings.CHAT_FLOOD_WAIT));

        if (this.entity.getType() != EntityType.PLAYER) {

            MessageComposer composer = null;

            if (this.entity.getType() == EntityType.PET) {
                composer = new TalkMessageComposer(this, type, message, 1);
            }

            if (this.entity.getType() == EntityType.BOT) {
                composer = new TalkMessageComposer(this, type, message, 2);
            }

            this.room.send(composer);
            return;
        }

        Player player = (Player)this.entity;
        RoomDao.saveChatlog(player, this.room.getData().getId(), type.name(), message);

        if (CommandManager.getInstance().hasCommand(player, message)) {
            CommandManager.getInstance().invokeCommand(player, message);
            return;
        } 

        InstantMessage playerMessage = new InstantMessage(this.entity.getDetails().getId(), -1, message);    {
            PluginManager.getInstance().callEvent(type.getEvent(), new LuaValue[] {
                CoerceJavaToLua.coerce(player),
                CoerceJavaToLua.coerce(this.room),
                CoerceJavaToLua.coerce(playerMessage)
            });

            message = playerMessage.getMessage();
        }

        MessageComposer composer = new TalkMessageComposer(this, type, message, this.chatColor);
        this.room.send(composer);

        for (Player person : this.room.getEntityManager().getPlayers()) {

            if (this.entity == person) {
                continue;
            }

            person.getRoomUser().lookTowards(this.entity.getRoomUser().getPosition());
        }
    }

    /**
     * Look towards specified position.
     *
     * @param look the position to look towards
     */
    public void lookTowards(Position look) {

        if (this.isWalking) {
            return;
        }

        int diff = this.getPosition().getRotation() - Rotation.calculate(this.position.getX(), this.position.getY(), look.getX(), look.getY());

        if ((this.getPosition().getRotation() % 2) == 0) {

            if (diff > 0) {
                this.position.setHeadRotation(this.getPosition().getRotation() - 1);
            } else if (diff < 0) {
                this.position.setHeadRotation(this.getPosition().getRotation() + 1);
            } else {
                this.position.setHeadRotation(this.getPosition().getRotation());
            }
        }

        this.lookResetTime = 6;
        this.needsUpdate = true;
    }

    /**
     * Warp to specified position.
     *
     * @param x the x
     * @param y the y
     * @param rotation the rotation
     */
    public void warpTo(int x, int y, int rotation) {

        if (this.room.getModel().hasInvalidCoordinates(x, y)) {
            return;
        }

        this.room.getMapping().getTile(this.position.getX(), this.position.getY()).removeEntity(this.entity);
        this.room.getMapping().getTile(x, y).addEntity(this.entity);

        this.position.setX(x);
        this.position.setY(y);
        this.position.setZ(this.room.getMapping().getTileHeight(x, y));
        this.position.setRotation(rotation);
        this.needsUpdate = true;
    }

    /**
     * Walk to specified position.
     *
     * @param X the x
     * @param Y the y
     */
    public void walkTo(int X, int Y) {

        if (!this.isWalkingAllowed) {
            return;
        }

        if (this.nextPosition != null) {
            this.position.setX(this.nextPosition.getX());
            this.position.setY(this.nextPosition.getY());
            this.updateNewHeight(this.position);
            this.needsUpdate = true;
        }

        RoomTile tile = this.room.getMapping().getTile(X, Y);

        if (tile != null && tile.getHighestItem() != null) {
            if (tile.getHighestItem().getDefinition().getInteractionType() == InteractionType.ONEWAYGATE) {
                return;
            }
        }

        this.goal.setX(X);
        this.goal.setY(Y);

        LinkedList<Position> path = Pathfinder.makePath(this.entity);

        if (path.size() > 0) {
            if (this.entity.getType() == EntityType.PLAYER) {
                PluginManager.getInstance().callEvent(PluginEvent.ROOM_WALK_REQUEST_EVENT, new LuaValue[] {
                    CoerceJavaToLua.coerce((Player)this.entity),
                    CoerceJavaToLua.coerce(this.room),
                    CoerceJavaToLua.coerce(this.position),
                    CoerceJavaToLua.coerce(this.goal)
                });
            }

            this.path = path;
            this.isWalking = true;
        }
    }


    /**
     * Update new height.
     *
     * @param position the position
     */
    public void updateNewHeight(Position position) {
        double height = this.room.getMapping().getTile(position.getX(), position.getY()).getHeight();
        this.position.setZ(height);
    }

    /**
     * Carry item.
     *
     * @param vendingId the vending id
     */
    public void carryItem(int vendingId) {

        if (vendingId == -1) {
            return;
        }

        this.carryTimer.set(0);
        this.carryItem = vendingId;

        if (vendingId > 0) {
            this.carryTimer.set(240);
        } else {
            this.carryTimer.set(-1);
        }

        this.room.send(new CarryObjectMessageComposer(this.virtualId, vendingId));
    }

    /**
     * Dispose.
     */
    public void clearUserData() {
        
        this.room = null;
        this.nextPosition = null;
        
        this.statuses = new HashMap<>();
        this.path = new LinkedList<>();

        this.position = new Position(0, 0, 0);
        this.goal = new Position(0, 0, 0);
        
        this.carryTimer = new AtomicInteger(-1);
        this.lookResetTime = -1;
        this.virtualId = -1;

        this.chatColor = 0;
        this.danceId = 0;
        this.carryItem = 0;

        this.needsUpdate = false;
        this.isWalking = false;
        this.isWalkingAllowed = true;

    }

    /**
     * Start dancing, will be ignored if the user has an effect activated.
     *
     * @param danceId the dance id
     */
    public void applyDance(int danceId) {

        if (this.danceId == danceId) {
            return;
        }
        
        if (this.effectId > 0) {
            return;
        }
        
        this.danceId = danceId;
        
        if (this.room != null) {
            this.room.send(new DanceMessageComposer(this.virtualId, this.danceId));
        }
    }

    /**
     * Apply effect, will force the user to stop dancing.
     *
     * @param effectId the effect id
     */
    public void applyEffect(int effectId) {

        if (this.effectId == effectId) {
            return;
        }

        this.effectId = effectId;

        if (this.danceId > 0) {
            this.applyDance(0);
        }
        
        if (this.room != null) {
            this.room.send(new DisplayEffectMessageComposer(this.virtualId, this.effectId));
        }
    }

    /**
     * Gets the position.
     *
     * @return the position
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Sets the position.
     *
     * @param position the new position
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Gets the tile.
     *
     * @return the tile
     */
    public RoomTile getTile() {
        return this.room.getMapping().getTile(this.position.getX(), this.position.getY());
    }

    /**
     * Gets the walking goal.
     *
     * @return the walking goal
     */
    public Position getGoal() {
        return goal;
    }

    /**
     * Sets the goal.
     *
     * @param goal the new goal
     */
    public void setGoal(Position goal) {
        this.goal = goal;
    }

    /**
     * Gets the next.
     *
     * @return the next
     */
    public Position getNextPosition() {
        return nextPosition;
    }

    /**
     * Sets the next.
     *
     * @param next the new next
     */
    public void setNextPosition(Position next) {
        this.nextPosition = next;
    }

    /**
     * Gets the virtual id.
     *
     * @return the virtual id
     */
    public int getVirtualId() {
        return virtualId;
    }

    /**
     * Sets the virtual id.
     *
     * @param virtualId the new virtual id
     */
    public void setVirtualId(int virtualId) {
        this.virtualId = virtualId;
    }

    /**
     * Gets the chat color.
     *
     * @return the chat color
     */
    public int getChatColor() {
        return chatColor;
    }

    /**
     * Sets the chat color.
     *
     * @param chatColor the new chat color
     */
    public void setChatColor(int chatColor) {
        this.chatColor = chatColor;
    }

    /**
     * Gets the dance id.
     *
     * @return the dance id
     */
    public int getDanceId() {
        return danceId;
    }

    /**
     * Gets the effect id.
     *
     * @return the effect id
     */
    public int getEffectId() {
        return effectId;
    }

    /**
     * Gets the statuses.
     *
     * @return the statuses
     */
    public HashMap<EntityStatus, String> getStatuses() {
        return statuses;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public LinkedList<Position> getPath() {
        return path;
    }

    /**
     * Sets the path.
     *
     * @param path the new path
     */
    public void setPath(LinkedList<Position> path) {

        if (this.path != null) {
            this.path.clear();
        }

        this.path = path;
    }

    /**
     * Needs update.
     *
     * @return true, if successful
     */
    public boolean getNeedsUpdate() {
        return needsUpdate;
    }

    /**
     * Sets the need update.
     *
     * @param needsWalkUpdate the new need update
     */
    public void setNeedUpdate(boolean needsWalkUpdate) {
        this.needsUpdate = needsWalkUpdate;
    }

    /**
     * Gets the room.
     *
     * @return the room
     */
    public Room getRoom() {
        return room;
    }

    /**
     * Gets the room id.
     *
     * @return the room id
     */
    public int getRoomId() {
        return (room == null ? 0 : room.getData().getId());
    }

    /**
     * Sets the room.
     *
     * @param room the new room
     */
    public void setRoom(Room room) {
        this.room = room;
    }

    /**
     * Checks if is walking.
     *
     * @return true, if is walking
     */
    public boolean isWalking() {
        return isWalking;
    }

    /**
     * Sets the walking.
     *
     * @param isWalking the new walking
     */
    public void setWalking(boolean isWalking) {
        this.isWalking = isWalking;
    }

    /**
     * Gets the entity.
     *
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Gets the look reset time.
     *
     * @return the look reset time
     */
    public int getLookResetTime() {
        return lookResetTime;
    }

    /**
     * Sets the look reset time.
     *
     * @param lookResetTime the new look reset time
     */
    public void setLookResetTime(int lookResetTime) {
        this.lookResetTime = lookResetTime;
    }

    /**
     * Gets the current item.
     *
     * @return the current item
     */
    public Item getCurrentItem() {
        return currentItem;
    }

    /**
     * Sets the current item.
     *
     * @param currentItem the new current item
     */
    public void setCurrentItem(Item currentItem) {
        this.currentItem = currentItem;
    }

    /**
     * Gets the carry timer.
     *
     * @return the carry timer
     */
    public AtomicInteger getCarryTimer() {
        return carryTimer;
    }

    /**
     * Gets the carry item.
     *
     * @return the carry item
     */
    public int getCarryItem() {
        return carryItem;
    }

    /**
     * Sets the carry item.
     *
     * @param carryItem the new carry item
     */
    public void setCarryItem(int carryItem) {
        this.carryItem = carryItem;
    }

    /**
     * Checks if is needs update.
     *
     * @return true, if is needs update
     */
    public boolean isNeedsUpdate() {
        return needsUpdate;
    }

    /**
     * Sets the needs update.
     *
     * @param needsUpdate the new needs update
     */
    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    /**
     * Checks if is teleporting.
     *
     * @return true, if is teleporting
     */
    public boolean isTeleporting() {
        return isTeleporting;
    }

    /**
     * Sets the teleporting.
     *
     * @param isTeleporting the new teleporting
     */
    public void setTeleporting(boolean isTeleporting) {
        this.isTeleporting = isTeleporting;
    }

    /**
     * Gets the teleport room id.
     *
     * @return the teleport room id
     */
    public int getTeleportRoomId() {
        return teleportRoomId;
    }

    /**
     * Sets the teleport room id.
     *
     * @param teleportRoomId the new teleport room id
     */
    public void setTeleportRoomId(int teleportRoomId) {
        this.teleportRoomId = teleportRoomId;
    }

    /**
     * Checks if is walking allowed.
     *
     * @return true, if is walking allowed
     */
    public boolean isWalkingAllowed() {
        return isWalkingAllowed;
    }

    /**
     * Sets the walking allowed.
     *
     * @param isWalkingAllowed the new walking allowed
     */
    public void setWalkingAllowed(boolean isWalkingAllowed) {
        this.isWalkingAllowed = isWalkingAllowed;
    }

    /**
     * Gets the requested room id.
     *
     * @return the requested room id
     */
    public int getRequestedRoomId() {
        return roomRequestedId;
    }

    /**
     * Sets the requested room id.
     *
     * @param roomRequestedId the new requested room id
     */
    public void setRequestedRoomId(int roomRequestedId) {
        this.roomRequestedId = roomRequestedId;
    }
}
