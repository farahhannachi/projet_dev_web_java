<?php

namespace App\Form\Extension;

use Symfony\Component\Form\AbstractTypeExtension;
use Symfony\Component\Form\Extension\Core\Type\FormType;
use Symfony\Component\Form\FormInterface;
use Symfony\Component\Form\FormView;
use Symfony\Component\OptionsResolver\OptionsResolver;

/**
 * Extension pour ajouter automatiquement accept-charset="UTF-8" à tous les formulaires
 */
class FormTypeExtension extends AbstractTypeExtension
{
    public function buildView(FormView $view, FormInterface $form, array $options): void
    {
        // Ajouter accept-charset="UTF-8" à tous les formulaires
        if ($form->isRoot()) {
            $view->vars['attr']['accept-charset'] = 'UTF-8';
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        // Options par défaut pour tous les formulaires
        $resolver->setDefaults([
            'attr' => ['accept-charset' => 'UTF-8'],
        ]);
    }

    public static function getExtendedTypes(): iterable
    {
        // Étendre tous les types de formulaires
        return [FormType::class];
    }
}
