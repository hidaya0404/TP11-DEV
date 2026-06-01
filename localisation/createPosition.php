<?php

header('Content-Type: application/json; charset=utf-8');

try {
    if ($_SERVER["REQUEST_METHOD"] !== "POST") {
        http_response_code(405);

        echo json_encode([
            "success" => false,
            "message" => "Méthode non autorisée. Utilisez POST."
        ]);

        exit;
    }

    require_once __DIR__ . '/service/PositionService.php';

    $latitude = $_POST['latitude'] ?? null;
    $longitude = $_POST['longitude'] ?? null;
    $datePosition = $_POST['date_position'] ?? null;
    $imei = $_POST['imei'] ?? null;

    if ($latitude === null || $longitude === null || $datePosition === null || $imei === null) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Champs manquants. Les champs requis sont : latitude, longitude, date_position et imei.",
            "received" => $_POST
        ]);

        exit;
    }

    if (!is_numeric($latitude) || !is_numeric($longitude)) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Latitude et longitude doivent être numériques."
        ]);

        exit;
    }

    $latitude = (double) $latitude;
    $longitude = (double) $longitude;

    if ($latitude < -90 || $latitude > 90) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Latitude invalide. Elle doit être entre -90 et 90."
        ]);

        exit;
    }

    if ($longitude < -180 || $longitude > 180) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "Longitude invalide. Elle doit être entre -180 et 180."
        ]);

        exit;
    }

    if (strlen($imei) > 50) {
        http_response_code(400);

        echo json_encode([
            "success" => false,
            "message" => "IMEI trop long. Maximum 50 caractères."
        ]);

        exit;
    }

    $service = new PositionService();

    $position = new Position(
        null,
        $latitude,
        $longitude,
        $datePosition,
        $imei
    );

    $result = $service->create($position);

    if ($result) {
        echo json_encode([
            "success" => true,
            "message" => "Position enregistrée avec succès."
        ]);
    } else {
        http_response_code(500);

        echo json_encode([
            "success" => false,
            "message" => "Erreur lors de l'enregistrement de la position."
        ]);
    }

} catch (Throwable $e) {
    http_response_code(500);

    echo json_encode([
        "success" => false,
        "message" => "Erreur serveur.",
        "debug" => $e->getMessage(),
        "file" => $e->getFile(),
        "line" => $e->getLine()
    ]);
}