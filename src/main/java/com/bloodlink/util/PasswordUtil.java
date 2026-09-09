package com.bloodlink.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private PasswordUtil() {}
    public static String hash(char[] password){
        try{
            byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
            PBEKeySpec spec=new PBEKeySpec(password,salt,120000,256);
            byte[] encoded=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return "120000:"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(encoded);
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public static boolean verify(char[] password,String stored){
        try{
            String[] p=stored.split(":"); int iterations=Integer.parseInt(p[0]);
            byte[] salt=Base64.getDecoder().decode(p[1]); byte[] expected=Base64.getDecoder().decode(p[2]);
            PBEKeySpec spec=new PBEKeySpec(password,salt,iterations,expected.length*8);
            byte[] actual=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            if(actual.length!=expected.length)return false; int diff=0; for(int i=0;i<actual.length;i++)diff|=actual[i]^expected[i]; return diff==0;
        }catch(Exception e){return false;}
    }
}
