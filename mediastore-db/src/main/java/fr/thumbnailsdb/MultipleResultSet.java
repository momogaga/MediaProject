package fr.thumbnailsdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * A wrapper class for ResultSets coming from different
 * databases
 *
 */
public class MultipleResultSet {

    ArrayList<Connection> connections = new ArrayList<Connection>();
    ArrayList<ResultSet> resultSets = new ArrayList<ResultSet>();

     public MultipleResultSet() {

     }

    public void add(Connection c, ResultSet r) {
        this.connections.add(c);
        this.resultSets.add(r);
    }

    public ArrayList<Connection> getConnections() {
        return connections;
    }

    public ArrayList<ResultSet> getResultSets() {
        return resultSets;
    }
}
