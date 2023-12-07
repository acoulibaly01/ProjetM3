/*
Copyright 2000- Francois de Bertrand de Beuvron

This file is part of CoursBeuvron.

CoursBeuvron is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoursBeuvron is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoursBeuvron.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.insa.coulibaly.projetm3.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Random;

/**
 *
 * @author francois
 */
public class GestionBDD {
        
    public static Connection connectGeneralMySQL(String host,
            int port, String database,
            String user, String pass)
            throws SQLException {
        Connection con = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port
                + "/" + database,
                user, pass);
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        return con;
    }
    
    public static String getPassPourServeurM3() {
        // en phase de développement, je vous conseille de mettre votre 
        // mot de passe en clair pour ne pas avoir à le retaper à chaque exécution
        // je ne veux pas mettre le mien dans ce programme que tout le monde
        // peut télécharger
//        return "monpass";
        // vous pouvez aussi le demander à chaque fois
//        return ConsoleFdB.entreeString("pass pour serveur M3 : ");
        // ici je le lit dans un fichier que j'ai exclu de git (.gitignore)
        try ( BufferedReader bin = new BufferedReader(new FileReader("pass.txt"))) {
            return bin.readLine();
        } catch (IOException ex) {
            throw new Error("impossible de lire le mot de passe", ex);
        }
    }
    
    public static Connection connectSurServeurM3() throws SQLException {
        return connectGeneralMySQL("92.222.25.165", 3306,
                "nom_de_votreBDD", "votre_Identifiant",
                "votre mot de passe");
    } // Le nom de votre base de données est identique à votre identifiant : m3_xxxxxx01
    
    /**
     * la base de donnée est crée en mémoire.
     * Elle est donc perdue (et recrée) à chaque démarrage du programme.
     * Il est clair que cela n'est utile qu'en phase de développement, ou comme
     * ici pour permettre de faire une petite démo sans vrai serveur, utilisateur
     * mot de passe
     * @return 
     */
    public static Connection connectSurBDDEnMemoire() throws SQLException {
        Connection con = DriverManager.getConnection(
                "jdbc:h2:mem:projM3",
                null,null);
        con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        // j'initialise la BdD si besoin (si la table li_client n'existe pas)
        // dans le cas d'une vraie BdD (pas en mémoire) la création du schéma
        // est sensé se faire une fois pour toute "hors ligne" et certainement
        // pas à chaque connection
        ResultSet tables = con.getMetaData().getTables(null, null, null, new String[] {"li_client","LI_CLIENT"});
        if (! tables.next()) {
            razBDD(con);
        }
        return con;
        
    }

    /**
     * Creation du schéma. On veut créer tout ou rien, d'où la gestion explicite
     * des transactions.
     *
     * @throws SQLException
     */
    public static void creeSchema(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try ( Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "create table li_client (\n"
                    + "    id integer not null primary key AUTO_INCREMENT,\n"
                    + "    nom varchar(30) not null unique,\n"
                    + "    pass varchar(30) not null\n"
                    + ")\n"
            );
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Suppression du schéma. Le schéma n'est peut-être pas créé, ou pas
     * entièrement créé, on ne s'arrête donc pas en cas d'erreur : on ne fait
     * que passer à la suite
     *
     * @throws SQLException
     */
    public static void deleteSchema(Connection conn) throws SQLException {
        try ( Statement st = conn.createStatement()) {
            // pour être sûr de pouvoir supprimer, il faut d'abord supprimer les liens
            // puis les tables
            // suppression des liens
            try {
                st.executeUpdate("drop table li_client");
            } catch (SQLException ex) {
            }
        }
    }
    
    public static void initTest(Connection conn) throws SQLException {
        inscription(conn, "toto");
    }
    
    public static void razBDD(Connection conn) throws SQLException {
        deleteSchema(conn);
        creeSchema(conn);
        initTest(conn);
    }
    
    public static Optional<String> inscription(Connection con, String nom) throws SQLException {
        try {
            con.setAutoCommit(false);
            if (testeLogin(con, nom)) {
                return Optional.empty();
            } else {
                try ( PreparedStatement pst = con.prepareStatement(
                        "insert into li_client (nom,pass) values (?,?)")) {
                    String pass = String.format("%08X", new Random().nextInt());
                    pst.setString(1, nom);
                    pst.setString(2, pass);
                    pst.executeUpdate();
                    return Optional.of(pass);
                }
            }
        } finally {
            con.setAutoCommit(true);
        }
    }
    
    public static boolean testeLogin(Connection con, String nom) throws SQLException {
        try ( PreparedStatement pst = con.prepareStatement(
                "select * from li_client where nom = ?")) {
            pst.setString(1, nom);
            try ( ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
    
    public static void debut() {
        try ( Connection con = connectSurServeurM3()) {
            System.out.println("connecté");
            razBDD(con);
            System.out.println("bdd cree");
        } catch (SQLException ex) {
            throw new Error("Connection impossible", ex);
        }
    }
    
    public static void main(String[] args) {
        debut();
    }

}
