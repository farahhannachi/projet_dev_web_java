<?php

namespace App\Exception;

/**
 * Exception thrown when file upload operations fail
 */
class FileUploadException extends \RuntimeException
{
    public function __construct(
        string $message = 'File upload failed',
        int $code = 0,
        ?\Throwable $previous = null
    ) {
        parent::__construct($message, $code, $previous);
    }
}
