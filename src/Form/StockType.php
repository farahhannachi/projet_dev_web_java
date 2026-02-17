<?php

namespace App\Form;

use App\Entity\Stock;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Validator\Constraints as Assert;

class StockType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('produit', EntityType::class, [
                'class' => 'App\Entity\Produit',
                'choice_label' => 'nom',
                'label' => 'Produit',
                'placeholder' => 'Sélectionner un produit',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Veuillez sélectionner un produit'])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'data-controller' => 'select2'
                ]
            ])
            ->add('depot', EntityType::class, [
                'class' => 'App\Entity\Depot',
                'choice_label' => 'nomDepot',
                'label' => 'Dépôt',
                'placeholder' => 'Sélectionner un dépôt',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Veuillez sélectionner un dépôt'])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'data-controller' => 'select2'
                ]
            ])
            ->add('quantite', Type\IntegerType::class, [
                'label' => 'Quantité initiale',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La quantité est obligatoire']),
                    new Assert\Positive(['message' => 'La quantité doit être supérieure à 0'])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'min' => 0,
                    'placeholder' => '0'
                ],
                'required' => true
            ])
            ->add('seuilAlerte', Type\IntegerType::class, [
                'label' => 'Seuil d\'alerte',
                'help' => 'Quantité minimale avant alerte',
                'constraints' => [
                    new Assert\Positive(['message' => 'Le seuil d\'alerte doit être positif'])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'min' => 0,
                    'placeholder' => '10'
                ],
                'required' => false
            ])
            ->add('seuilCritique', Type\IntegerType::class, [
                'label' => 'Seuil critique',
                'help' => 'Quantité critique (rupture de stock)',
                'constraints' => [
                    new Assert\Positive(['message' => 'Le seuil critique doit être positif'])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'min' => 0,
                    'placeholder' => '5'
                ],
                'required' => false
            ])
            ->add('dateEntree', Type\DateType::class, [
                'label' => 'Date d\'entrée',
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date d\'entrée est obligatoire'])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'data-controller' => 'flatpickr'
                ],
                'required' => true
            ])
            ->add('dateExpiration', Type\DateType::class, [
                'label' => 'Date d\'expiration',
                'widget' => 'single_text',
                'required' => false,
                'attr' => [
                    'class' => 'form-control',
                    'data-controller' => 'flatpickr',
                    'placeholder' => 'Optionnel'
                ],
                'help' => 'Laissez vide si non applicable'
            ])
            ->add('emplacement', Type\TextType::class, [
                'label' => 'Emplacement',
                'required' => false,
                'constraints' => [
                    new Assert\Length([
                        'max' => 100,
                        'maxMessage' => 'L\'emplacement ne peut pas dépasser 100 caractères'
                    ])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'placeholder' => 'Ex: Allée A, Rangée 3, Étagère 2'
                ],
                'help' => 'Localisation précise dans le dépôt'
            ])
            ->add('batchNumber', Type\TextType::class, [
                'label' => 'Numéro de lot',
                'required' => false,
                'constraints' => [
                    new Assert\Length([
                        'max' => 50,
                        'maxMessage' => 'Le numéro de lot ne peut pas dépasser 50 caractères'
                    ])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'placeholder' => 'Ex: LOT-2024-001'
                ],
                'help' => 'Pour la traçabilité'
            ])
            ->add('fournisseur', Type\TextType::class, [
                'label' => 'Fournisseur',
                'required' => false,
                'constraints' => [
                    new Assert\Length([
                        'max' => 100,
                        'maxMessage' => 'Le nom du fournisseur ne peut pas dépasser 100 caractères'
                    ])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'placeholder' => 'Nom du fournisseur'
                ]
            ])
            ->add('notes', Type\TextareaType::class, [
                'label' => 'Notes',
                'required' => false,
                'constraints' => [
                    new Assert\Length([
                        'max' => 1000,
                        'maxMessage' => 'Les notes ne peuvent pas dépasser 1000 caractères'
                    ])
                ],
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 3,
                    'placeholder' => 'Notes supplémentaires sur ce stock...'
                ]
            ])
            ->add('etatStock', Type\ChoiceType::class, [
                'label' => 'État du stock',
                'choices' => [
                    'Disponible' => Stock::ETAT_DISPONIBLE,
                    'Alerte' => Stock::ETAT_ALERTE,
                    'Rupture' => Stock::ETAT_RUPTURE,
                    'Périmé' => Stock::ETAT_PERIME,
                    'Expiré' => Stock::ETAT_EXPIRE,
                ],
                'attr' => [
                    'class' => 'form-control'
                ],
                'required' => true
            ])
            ->add('dateDerniereMiseAJour', Type\DateTimeType::class, [
                'label' => 'Dernière mise à jour',
                'widget' => 'single_text',
                'disabled' => true,
                'data' => new \DateTime(),
                'attr' => [
                    'class' => 'form-control',
                    'readonly' => true
                ],
                'required' => false,
                'help' => 'Mis à jour automatiquement'
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Stock::class,
            'attr' => [
                'novalidate' => 'novalidate'
            ]
        ]);
    }
}
