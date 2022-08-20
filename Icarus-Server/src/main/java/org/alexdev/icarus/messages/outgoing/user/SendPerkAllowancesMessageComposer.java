package org.alexdev.icarus.messages.outgoing.user;

import org.alexdev.icarus.messages.headers.Outgoing;
import org.alexdev.icarus.messages.types.MessageComposer;
import org.alexdev.icarus.server.api.messages.Response;
import org.alexdev.icarus.util.config.Configuration;

public class SendPerkAllowancesMessageComposer extends MessageComposer {

    @Override
    public void compose(Response response) {
        //response.init(Outgoing.SendPerkAllowancesMessageComposer);
        response.writeInt(16);
        response.writeString("USE_GUIDE_TOOL");
        response.writeString("");
        response.writeBool(false);
        response.writeString("GIVE_GUIDE_TOURS");
        response.writeString("requirement.unfulfilled.helper_le");
        response.writeBool(false);
        response.writeString("JUDGE_CHAT_REVIEWS");
        response.writeString("");
        response.writeBool(true);
        response.writeString("VOTE_IN_COMPETITIONS");
        response.writeString("");
        response.writeBool(true);
        response.writeString("CALL_ON_HELPERS");
        response.writeString("");
        response.writeBool(false);
        response.writeString("CITIZEN");
        response.writeString("");
        response.writeBool(true);
        response.writeString("TRADE");
        response.writeString("");
        response.writeBool(true);
        response.writeString("HEIGHTMAP_EDITOR_BETA");
        response.writeString("");
        response.writeBool(false);
        response.writeString("EXPERIMENTAL_CHAT_BETA");
        response.writeString("requirement.unfulfilled.helper_level_2");
        response.writeBool(true);
        response.writeString("EXPERIMENTAL_TOOLBAR");
        response.writeString("");
        response.writeBool(true);
        response.writeString("BUILDER_AT_WORK");
        response.writeString("");
        response.writeBool(true);
        response.writeString("NAVIGATOR_PHASE_ONE_2014");
        response.writeString("");
        response.writeBool(false);
        response.writeString("CAMERA");
        response.writeString("");
        response.writeBool(Configuration.getInstance().getGameConfig().get("Camera", "camera.enabled", Boolean.class));
        response.writeString("NAVIGATOR_PHASE_TWO_2014");
        response.writeString("");
        response.writeBool(true);
        response.writeString("MOUSE_ZOOM");
        response.writeString("");
        response.writeBool(true);
        response.writeString("NAVIGATOR_ROOM_THUMBNAIL_CAMERA");
        response.writeString("");
        response.writeBool(Configuration.getInstance().getGameConfig().get("Thumbnail", "thumbnail.create.enabled", Boolean.class));

    }

    @Override
    public short getHeader() {
        return Outgoing.SendPerkAllowancesMessageComposer;
    }
}
