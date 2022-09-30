package storagemanager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class FileManager {

    public static String readChars(DataInputStream in) throws IOException {
        int len = in.readInt();
        String finalString = "";
        for(int i = 0; i < len; i ++){
            char c = in.readChar();
            finalString += c;
        }
        return finalString;
    }

    public static void writeChars(String outputString, DataOutputStream out) throws IOException {
        out.writeInt(outputString.length());
        for(int i = 0; i < outputString.length(); i ++){
            out.writeChar(outputString.charAt(i));
        }
    }


}
