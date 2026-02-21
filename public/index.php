<?php

use App\Kernel;

// Set default charset to UTF-8
ini_set('default_charset', 'UTF-8');
mb_internal_encoding('UTF-8');

require_once dirname(__DIR__).'/vendor/autoload_runtime.php';

return function (array $context) {
    return new Kernel($context['APP_ENV'], (bool) $context['APP_DEBUG']);
};
