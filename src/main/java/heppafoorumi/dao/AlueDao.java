package heppafoorumi.dao;

import heppafoorumi.domain.Alue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import heppafoorumi.database.Database;
import heppafoorumi.domain.Aihe;
import heppafoorumi.domain.Alueraportti;
import heppafoorumi.domain.Viesti;
import java.sql.Timestamp;

public class AlueDao implements Dao<Alue, Integer> {

    private final Database database;
    private KaikkiDao kaikkiDao;

    public AlueDao(Database database) {
        this.database = database;
        this.kaikkiDao = null;
    }

    public KaikkiDao getKaikkiDao() {
        return kaikkiDao;
    }

    public void setKaikkiDao(KaikkiDao kaikkiDao) {
        this.kaikkiDao = kaikkiDao;
    }

    @Override
    public Alue findOne(Integer key) throws SQLException {
        Connection connection = database.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM Alue WHERE id = ?");
        statement.setObject(1, key);

        ResultSet resultSet = statement.executeQuery();

        if (!resultSet.next()) {
            resultSet.close();
            statement.close();
            connection.close();
            return null;
        }

        Integer id = resultSet.getInt("id");
        Timestamp aikaleima = resultSet.getTimestamp("aikaleima");
        String otsikko = resultSet.getString("otsikko");
        String teksti = resultSet.getString("teksti");

        Alue alue = new Alue(this.database, id, aikaleima, otsikko, teksti);

        resultSet.close();
        statement.close();
        connection.close();

        return alue;
    }

    @Override
    public List<Alue> findAll() throws SQLException {

        Connection connection = database.getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM Alue");

        List<Alue> alueet = new ArrayList();
        while (resultSet.next()) {
            Integer id = resultSet.getInt("id");
            Timestamp aikaleima = resultSet.getTimestamp("aikaleima");
            String otsikko = resultSet.getString("otsikko");
            String teksti = resultSet.getString("teksti");

            alueet.add(new Alue(this.database, id, aikaleima, otsikko, teksti));
        }

        resultSet.close();
        connection.close();

        return alueet;
    }

    public List<Alueraportti> findTarpeellisetTiedot() throws SQLException {
        Connection connection = database.getConnection();
        ResultSet resultSet = connection.createStatement().executeQuery(
                "SELECT * FROM Alue AS alue "
                + "    LEFT JOIN Aihe AS aihe "
                + "        ON (aihe.alue = alue.id) "
                + "    WHERE (aihe.id = "
                + "        (SELECT MAX(id) FROM aihe AS uusin_aihe "
                + "            WHERE uusin_aihe.alue = alue.id) "
                + "        OR aihe.id IS NULL) "
                + "        ORDER BY alue.otsikko");

        List<Alueraportti> raporttilista = new ArrayList();

        while (resultSet.next()) {
            Integer alueId = resultSet.getInt("alue.id");
            Timestamp alueAikaleima = resultSet.getTimestamp("alue.aikaleima");
            String alueOtsikko = resultSet.getString("alue.otsikko");
            String alueTeksti = resultSet.getString("alue.teksti");

            Integer aiheId = resultSet.getInt("aihe.id");
            Timestamp aiheAikaleima = resultSet.getTimestamp("aihe.aikaleima");
            Integer aiheAlue = resultSet.getInt("aihe.alue");
            String aiheNimimerkki = resultSet.getString("aihe.nimimerkki");
            String aiheOtsikko = resultSet.getString("aihe.otsikko");
            String aiheTeksti = resultSet.getString("aihe.teksti");

            // Etsitään nykyisen alueen uusin viesti.
            Viesti uusinViesti = this.kaikkiDao.getViestiDao().findUusinViesti(alueId);

            Alue alue = new Alue(this.database, alueId, alueAikaleima, alueOtsikko, alueTeksti);
            Aihe uusinAihe = new Aihe(this.database, aiheId, aiheAikaleima, aiheAlue, aiheNimimerkki, aiheOtsikko, aiheTeksti);

            int alueenViestienLkm = 0;

            for (Aihe aihe : this.getKaikkiDao().getAiheDao().findAll(alueId)) {
                alueenViestienLkm += this.kaikkiDao.getViestiDao().findAll(aihe.getId()).size();
            }

            Integer aiheidenLkm = this.kaikkiDao.getAiheDao().findLkm(alueId);
            raporttilista.add(new Alueraportti(alue, uusinAihe, uusinViesti, aiheidenLkm, alueenViestienLkm, null, null));
        }

        resultSet.close();
        connection.close();

        return raporttilista;
    }

    @Override
    public void delete(Integer key) throws SQLException {
        Connection connection = database.getConnection();
        PreparedStatement statement = connection.prepareStatement("DELETE FROM Alue WHERE id = ?");
        statement.setObject(1, key);

        statement.executeUpdate();

        statement.close();
        connection.close();
    }

    public void create(String otsikko, String teksti) throws SQLException {
        Connection connection = database.getConnection();
        
        //Jos merkkijonot ovat sallittua pidempiä, pätkäistään loppu pois
        if (teksti.length() > 200) {
            teksti = teksti.substring(0, 200);
        }
        
        if (otsikko.length() > 200) {
            otsikko = otsikko.substring(0, 200);
        }

        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Alue (aikaleima, otsikko, teksti) VALUES(?, ?, ?)");
        statement.setObject(1, new java.sql.Timestamp(new java.util.Date().getTime()));
        statement.setObject(2, otsikko);
        statement.setObject(3, teksti);

        statement.executeUpdate();

        statement.close();
        connection.close();
    }
}
