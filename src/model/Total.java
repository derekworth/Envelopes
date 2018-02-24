/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model;

/**
 *
 * @author Derek
 */
public class Total extends Container {
    public Total(int amt) {
        super(-1, "N/A", "N/A", "TOTAL", amt);
    }
    
    @Override
    public void setName(String name) { /*DO NOTHING*/ }
}