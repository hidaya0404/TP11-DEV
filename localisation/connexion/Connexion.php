<?php

class Connexion {
    private $connexion;

    public function __construct() {
        $host = '127.0.0.1';
        $port = '3307';
        $dbname = 'localisation';
        $login = 'root';
        $password = 'root';

        try {
            $this->connexion = new PDO(
                "mysql:host=$host;port=$port;dbname=$dbname;charset=utf8mb4",
                $login,
                $password,
                [
                    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
                ]
            );
        } catch (PDOException $e) {
            error_log("Erreur PDO : " . $e->getMessage());
            throw new Exception("Erreur de connexion à la base de données.");
        }
    }

    public function getConnexion() {
        return $this->connexion;
    }
}