<?php

namespace App\EventSubscriber;

use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\Form\FormEvent;
use Symfony\Component\Form\FormEvents;

/**
 * Event Subscriber pour forcer l'encodage UTF-8 sur tous les formulaires
 * et nettoyer les données mal encodées
 */
class FormEncodingSubscriber implements EventSubscriberInterface
{
    public static function getSubscribedEvents(): array
    {
        return [
            FormEvents::PRE_SUBMIT => 'onPreSubmit',
        ];
    }

    public function onPreSubmit(FormEvent $event): void
    {
        $data = $event->getData();
        
        if (!is_array($data)) {
            return;
        }
        
        // Nettoyer récursivement toutes les chaînes de caractères
        $cleanedData = $this->cleanEncodingRecursive($data);
        
        $event->setData($cleanedData);
    }
    
    /**
     * Nettoie l'encodage des données de manière récursive
     */
    private function cleanEncodingRecursive($data)
    {
        if (is_array($data)) {
            foreach ($data as $key => $value) {
                $data[$key] = $this->cleanEncodingRecursive($value);
            }
            return $data;
        }
        
        if (is_string($data)) {
            return $this->fixEncoding($data);
        }
        
        return $data;
    }
    
    /**
     * Corrige l'encodage d'une chaîne de caractères
     */
    private function fixEncoding(string $text): string
    {
        // Si la chaîne est déjà en UTF-8 valide, la retourner telle quelle
        if (mb_check_encoding($text, 'UTF-8')) {
            // Vérifier si c'est du double encodage
            if (strpos($text, 'Ã') !== false || strpos($text, 'â€') !== false) {
                // Essayer de corriger le double encodage
                $decoded = utf8_decode($text);
                if (mb_check_encoding($decoded, 'UTF-8')) {
                    return $decoded;
                }
            }
            return $text;
        }
        
        // Essayer de convertir depuis ISO-8859-1
        $converted = mb_convert_encoding($text, 'UTF-8', 'ISO-8859-1');
        if (mb_check_encoding($converted, 'UTF-8')) {
            return $converted;
        }
        
        // Essayer de convertir depuis Windows-1252
        $converted = mb_convert_encoding($text, 'UTF-8', 'Windows-1252');
        if (mb_check_encoding($converted, 'UTF-8')) {
            return $converted;
        }
        
        // En dernier recours, forcer l'UTF-8 en ignorant les erreurs
        return mb_convert_encoding($text, 'UTF-8', 'UTF-8');
    }
}
