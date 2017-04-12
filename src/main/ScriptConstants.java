package main;

class ScriptConstants {

    static final String ADD_TO_STARTUP =
            "tell application \"System Events\" \n" +
                    "  make new login item at end of login items with properties" +
                    " {name:\"Menify\", path:(POSIX path of (path to application \"Menify\")), hidden:true}\n" +
                    "end tell";

    static final String REMOVE_FROM_STARTUP = "tell application \"System Events\" to delete login item \"Menify\"";

    static final String SPOTIFY_META_DATA_SCRIPT = "tell application \"System Events\"\n" +
            "  set myList to (name of every process)\n" +
            "end tell\n" +
            "if myList contains \"Spotify\" then\n" +
            "  tell application \"Spotify\"\n" +
            "    if player state is paused then\n" +
            "      set output to \"Music paused\"\n" +
            "    else\n" +
            "      set trackname to name of current track\n" +
            "      set artistname to artist of current track\n" +
            "      set albumname to album of current track\n" +
            "      if player state is playing then\n" +
            "        set output to artistname & \" - \" & trackname & \"\"\n" +
            "      else if player state is paused then\n" +
            "        set output to artistname & \" - \" & trackname & \"\"\n" +
            "      end if\n" +
            "    end if\n" +
            "  end tell\n" +
            "else\n" +
            "  set output to \"Spotify not running\"\n" +
            "end if";

}
