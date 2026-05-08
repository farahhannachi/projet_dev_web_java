<?php

namespace App\Form;

use App\Entity\Commande;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\MoneyType;
use Symfony\Component\Form\Extension\Core\Type\TelType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class CommandeType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom complet',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le nom est obligatoire']),
                    new Assert\Length(['max' => 255]),
                ],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Email',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'L\'email est obligatoire']),
                    new Assert\Email(['message' => 'Email invalide']),
                ],
            ])
            ->add('telephone', TelType::class, [
                'label' => 'Telephone',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le telephone est obligatoire']),
                    new Assert\Length(['max' => 20]),
                ],
            ])
            ->add('adresseLivraison', TextType::class, [
                'label' => 'Adresse de livraison',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'L\'adresse est obligatoire']),
                    new Assert\Length(['max' => 255]),
                ],
            ])
            ->add('modePaiement', ChoiceType::class, [
                'label' => 'Mode de paiement',
                'choices' => [
                    'Paiement a la livraison' => 'livraison',
                    'Paiement en ligne' => 'en_ligne',
                ],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Choisissez un mode de paiement']),
                ],
            ])
            ->add('statut', ChoiceType::class, [
                'label' => 'Statut',
                'choices' => [
                    'En attente' => 'en_attente',
                    'Confirmee' => 'confirmee',
                    'Annulee' => 'annulee',
                    'Livree' => 'livree',
                    'Review anti-fraude' => 'review',
                    'Bloquee' => 'bloquee',
                ],
            ])
            ->add('total', MoneyType::class, [
                'label' => 'Total',
                'currency' => 'DT',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le total est obligatoire']),
                    new Assert\PositiveOrZero(['message' => 'Le total doit etre positif']),
                ],
            ])
            ->add('message', TextareaType::class, [
                'label' => 'Message (optionnel)',
                'required' => false,
                'attr' => ['rows' => 4],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Commande::class,
        ]);
    }
}
