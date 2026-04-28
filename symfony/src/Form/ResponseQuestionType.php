<?php

namespace App\Form;

use App\Entity\Question;
use App\Entity\ResponseQuestion;
use App\Entity\Utilisateur;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;

class ResponseQuestionType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        // Champ commun pour tous les utilisateurs
        $builder
            ->add('reponseText', TextareaType::class, [
                'label' => 'Votre réponse',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 5,
                    'placeholder' => 'Tapez votre réponse...',
                ],
            ]);

        // Champs uniquement pour les admins
        if ($options['is_admin']) {
            $builder
                ->add('reponseRole', ChoiceType::class, [
                    'label' => 'Type de réponse',
                    'choices' => [
                        'Question' => 'question',
                        'Information' => 'info',
                        'Demande de preuve' => 'demande_preuve',
                        'Solution' => 'solution',
                        'Décision' => 'decision',
                    ],
                    'attr' => ['class' => 'form-select'],
                ])
                ->add('actionType', ChoiceType::class, [
                    'label' => 'Action',
                    'choices' => [
                        'Aucune' => 'aucune',
                        'Remboursement' => 'remboursement',
                        'Remplacement' => 'remplacement',
                        'Retour accepté' => 'retour_accepte',
                        'Retour refusé' => 'retour_refuse',
                        'Escalade' => 'escalade',
                    ],
                    'attr' => ['class' => 'form-select'],
                    'data' => 'aucune',
                ])
                ->add('impactStatut', ChoiceType::class, [
                    'label' => 'Impact sur le statut',
                    'choices' => [
                        'Aucun' => 'aucun',
                        'En cours' => 'en_cours',
                        'Résolu' => 'resolu',
                        'Fermé' => 'ferme',
                    ],
                    'attr' => ['class' => 'form-select'],
                    'data' => 'aucun',
                ]);
        }

        // Champ fichier pour tous
        $builder
            ->add('fichier', FileType::class, [
                'label' => 'Pièce jointe (optionnelle)',
                'mapped' => false,
                'required' => false,
                'constraints' => [
                    new File([
                        'maxSize' => '5M',
                        'mimeTypes' => [
                            'image/jpeg',
                            'image/png',
                            'image/gif',
                            'application/pdf',
                            'application/msword',
                            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                        ],
                        'mimeTypesMessage' => 'Veuillez télécharger un fichier valide (JPG, PNG, GIF, PDF, DOC, DOCX)',
                    ])
                ],
                'attr' => ['class' => 'form-control'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => ResponseQuestion::class,
            'is_admin' => false, // Par défaut, ce n'est pas un admin
        ]);
    }
}
