<?php

require_once __DIR__ . '/../dao/IDao.php';
require_once __DIR__ . '/../classe/Position.php';
require_once __DIR__ . '/../connexion/Connexion.php';

class PositionService implements IDao {
    private $connexion;

    public function __construct() {
        $this->connexion = new Connexion();
    }

    public function create($position) {
        $sql = "INSERT INTO `position`(`latitude`, `longitude`, `date_position`, `imei`)
                VALUES(:latitude, :longitude, :date_position, :imei)";

        $stmt = $this->connexion->getConnexion()->prepare($sql);

        return $stmt->execute([
            ':latitude' => $position->getLatitude(),
            ':longitude' => $position->getLongitude(),
            ':date_position' => $position->getDatePosition(),
            ':imei' => $position->getImei()
        ]);
    }

    public function update($obj) {
        throw new BadMethodCallException("La méthode update() n'est pas encore implémentée.");
    }

    public function delete($obj) {
        throw new BadMethodCallException("La méthode delete() n'est pas encore implémentée.");
    }

    public function getById($obj) {
        throw new BadMethodCallException("La méthode getById() n'est pas encore implémentée.");
    }

    public function getAll() {
        throw new BadMethodCallException("La méthode getAll() n'est pas encore implémentée.");
    }
}