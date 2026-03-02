<?php

namespace App\Form;

use App\Entity\Question;
use App\Entity\Utilisateur;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\File;

class QuestionType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $isAdmin = $options['is_admin'];

        $builder
            ->add('typeTicket', ChoiceType::class, [
                'label' => 'Type de ticket',
                'choices' => [
                    'Support' => 'support',
                    'Réclamation' => 'reclamation',
                    'Retour' => 'retour',
                ],
                'attr' => ['class' => 'form-select'],
            ])
            ->add('objet', TextType::class, [
                'label' => 'Objet',
                'attr' => ['class' => 'form-control', 'placeholder' => 'Résumé de votre demande'],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 6,
                    'placeholder' => 'Décrivez votre problème en détail...',
                ],
            ]);

        if ($isAdmin) {
            $builder
                ->add('priorite', ChoiceType::class, [
                    'label' => 'Priorité',
                    'choices' => [
                        'Basse' => 'basse',
                        'Normale' => 'normale',
                        'Haute' => 'haute',
                    ],
                    'attr' => ['class' => 'form-select'],
                ])
                ->add('statut', ChoiceType::class, [
                    'label' => 'Statut',
                    'choices' => [
                        'Ouvert' => 'ouvert',
                        'En cours' => 'en_cours',
                        'Résolu' => 'resolu',
                        'Fermé' => 'ferme',
                    ],
                    'attr' => ['class' => 'form-select'],
                ]);
        }

        $builder->add('fichier', FileType::class, [
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
            'data_class' => Question::class,
            'is_admin' => false,
        ]);
    }
}
