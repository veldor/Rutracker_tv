package net.veldor.rutrackertv.selections;

public class ContentTypes {

    private static String[] movieAnchors = new String[]{"WEBRip", "DVDRip", "HDRip", "BDRip", "WEB-DLRip", "MVO", "DUB", "AVO"};
    private static String[] formats = new String[]{".mkv", ".avi", ".vob", ".mp4", ".mov", ".ts", ".mpeg", ".m4v", ".h264"};

    public static boolean isMovie(String name) {
        if(name != null){
            for(String s : movieAnchors){
                if(name.contains(s)){
                    return true;
                }
            }
            for(String s : formats){
                if(name.equals(s.substring(1))){
                    return true;
                }
            }
        }
        return false;
    }
    public static String getMovieFormat(String name) {
        for(String s : formats){
            if(name.endsWith(s)){
                return s.substring(1);
            }
        }
        return null;
    }

    public static String getContentType(String name) {
        for(String s : movieAnchors){
            if(name.contains(s)){
                return s;
            }
        }
        return null;
    }
}
