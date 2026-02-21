<?php

namespace App\Controller;

use App\Entity\Address;
use App\Form\AddressType;
use App\Repository\AddressRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[Route('/address')]
#[IsGranted('ROLE_USER')]
class AddressController extends AbstractController
{
    #[Route('/', name: 'app_mes_adresses', methods: ['GET'])]
    public function index(AddressRepository $addressRepository): Response
    {
        $user = $this->getUser();
        $addresses = $addressRepository->findBy(['utilisateur' => $user], ['id' => 'DESC']);

        return $this->render('front/profile/addresses.html.twig', [
            'addresses' => $addresses,
        ]);
    }

    #[Route('/new', name: 'app_address_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager): Response
    {
        $user = $this->getUser();
        
        $address = new Address();
        $address->setUtilisateur($user);
        
        $form = $this->createForm(AddressType::class, $address);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->persist($address);
            $entityManager->flush();

            $this->addFlash('success', 'Adresse ajoutée avec succès.');

            return $this->redirectToRoute('app_mes_adresses');
        }

        return $this->render('front/profile/address_form.html.twig', [
            'address' => $address,
            'form' => $form,
        ]);
    }

    #[Route('/{id}/edit', name: 'app_address_edit', methods: ['GET', 'POST'])]
    public function edit(Request $request, Address $address, EntityManagerInterface $entityManager): Response
    {
        // Check if the address belongs to the current user
        if ($address->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous ne pouvez pas modifier cette adresse.');
        }

        $form = $this->createForm(AddressType::class, $address);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();

            $this->addFlash('success', 'Adresse mise à jour avec succès.');

            return $this->redirectToRoute('app_mes_adresses');
        }

        return $this->render('front/profile/address_form.html.twig', [
            'address' => $address,
            'form' => $form,
        ]);
    }

    #[Route('/{id}/delete', name: 'app_address_delete', methods: ['POST'])]
    public function delete(Request $request, Address $address, EntityManagerInterface $entityManager): Response
    {
        // Check if the address belongs to the current user
        if ($address->getUtilisateur() !== $this->getUser()) {
            throw $this->createAccessDeniedException('Vous ne pouvez pas supprimer cette adresse.');
        }

        $entityManager->remove($address);
        $entityManager->flush();
        $this->addFlash('success', 'Adresse supprimée avec succès.');

        return $this->redirectToRoute('app_mes_adresses');
    }
}
