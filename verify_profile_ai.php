<?php
require_once __DIR__ . '/vendor/autoload.php';
use App\Kernel;
use App\Entity\Utilisateur;
use App\Controller\ProfileController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Security\Core\Authentication\Token\UsernamePasswordToken;
use Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface;

$kernel = new Kernel('dev', true);
$kernel->boot();
$container = $kernel->getContainer();

// Find the user
$entityManager = $container->get('doctrine.orm.entity_manager');
$user = $entityManager->getRepository(Utilisateur::class)->findOneBy(['email' => 'ihebjbir10@gmail.com']);

if (!$user) {
    echo "User not found\n";
    exit(1);
}

// Manually set the security context
$token = new UsernamePasswordToken($user, 'main', $user->getRoles());
$container->get('security.token_storage')->setToken($token);

// Instantiate the controller (manual instantiation because of constructor)
$twoFactor = $container->get(App\Service\TwoFactorAuthService::class);
$httpClient = $container->get('http_client');
$controller = new ProfileController($twoFactor, $entityManager, $httpClient);
// We also need to set the container for AbstractController methods like json()
$controller->setContainer($container);

$request = new Request([], [], [], [], [], [], json_encode(['age' => 23, 'taille' => 170]));
$request->headers->set('Content-Type', 'application/json');

try {
    echo "Calling aiHealthSummary...\n";
    $response = $controller->aiHealthSummary(
        $request,
        $container->get(App\Repository\TraitementRepository::class),
        $container->get(App\Repository\OrdonnanceRepository::class)
    );
    echo "Response Code: " . $response->getStatusCode() . "\n";
    echo "Response Content: " . $response->getContent() . "\n";
} catch (\Throwable $e) {
    echo "CRITICAL ERROR: " . $e->getMessage() . "\n";
    echo "In " . $e->getFile() . " on line " . $e->getLine() . "\n";
    echo $e->getTraceAsString() . "\n";
}
?>
