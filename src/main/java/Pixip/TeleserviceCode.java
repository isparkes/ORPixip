/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Pixip;

/**
 *
 * @author ian
 */
public enum TeleserviceCode {
  VOICE,
  GPRS,
  SMS,
  UNKNOWN;
  
    public String value() {
        return name();
    }

    public static TeleserviceCode fromValue(String v) {
        return valueOf(v);
    }
}
