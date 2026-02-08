<?php
// Router for PHP built-in server
// This file is only used when running the app with the built-in PHP server

// Decode the URL
$uri = urldecode(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH));

// If it's a real file (CSS, JS, images, etc.), serve it directly
if ($uri !== '/' && file_exists(__DIR__ . $uri)) {
    return false;
}

// Otherwise, route everything through index.php
require __DIR__ . '/index.php';
