<?php

namespace App\Form;

use App\Entity\Utilisateur;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\PasswordType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\IsTrue;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Email;
use Symfony\Component\Form\Extension\Core\Type\TextType;

class RegistrationFormType extends AbstractType
{
   public function buildForm(FormBuilderInterface $builder, array $options): void
   {
    $builder
        ->add('nom', TextType::class, [
            'label' => 'Nom',
            'attr' => [
                'placeholder' => 'Entrez votre nom'
            ]
        ])
        ->add('prenom', TextType::class, [
            'label' => 'Prénom',
            'attr' => [
                'placeholder' => 'Entrez votre prénom'
            ]
        ])
        ->add('email', EmailType::class, [
            'label' => 'Adresse email',
            'attr' => [
                'placeholder' => 'exemple@gmail.com'
            ],
            'constraints' => [
                new NotBlank([
                    'message' => 'Veuillez entrer une adresse email',
                ]),
                new Email([
                    'message' => 'Veuillez entrer une adresse email valide (exemple: nom@domaine.com)',
                ]),
            ],
        ])
        ->add('agreeTerms', CheckboxType::class, [
            'mapped' => false,
            'label' => 'J\'accepte les conditions d\'utilisation',
            'constraints' => [
                new IsTrue([
                    'message' => 'Vous devez accepter les conditions d\'utilisation',
                ]),
            ],
        ])
        ->add('plainPassword', PasswordType::class, [
            'mapped' => false,
            'attr' => ['autocomplete' => 'new-password', 'placeholder' => 'Entrez votre mot de passe'],
            'label' => 'Mot de passe',
            'constraints' => [
                new NotBlank([
                    'message' => 'Veuillez entrer un mot de passe',
                ]),
                new Length([
                    'min' => 6,
                    'minMessage' => 'Votre mot de passe doit contenir au moins {{ limit }} caractères',
                    'max' => 4096,
                ]),
            ],
        ]);
   }

   public function configureOptions(OptionsResolver $resolver): void
   {
       $resolver->setDefaults([
           'data_class' => Utilisateur::class,
       ]);
   }
}
