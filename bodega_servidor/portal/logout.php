<?php
require_once __DIR__ . '/_base.php';
$_SESSION = [];
session_destroy();
header('Location: login.php');
