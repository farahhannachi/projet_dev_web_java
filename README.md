# Curavita – Online Pharmacy Platform

---

## Academic Context

This project was developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

Developed at **Esprit School of Engineering – Tunisia**.

---

## Overview

Curavita is a full-stack web application developed by **Team One Health** as an online pharmacy platform.

It allows users to browse medications, place orders, and manage prescriptions in a secure and user-friendly environment.

The project aims to simplify access to healthcare products and digitalize pharmacy services while ensuring reliability and efficiency.

---

## Features

- 🛒 Online purchase of medications  
- 🔍 Search and filter pharmaceutical products  
- 👤 User account management (signup/login)  
- 📦 Order management and tracking  
- 💊 Prescription submission and validation  
- 🧾 Complaint and feedback system  
- 🔐 Secure data handling  

---

## Tech Stack

### Frontend
- HTML5
- CSS3
- JavaScript
- Bootstrap

### Backend
- PHP
- Symfony Framework
- MySQL

---

## Architecture (MVC)

Curavita follows the **MVC (Model–View–Controller)** architecture.

### Model
Manages data and business logic using Symfony Entities and MySQL database.

### View
Handles the user interface using Twig, HTML, CSS, and Bootstrap.

### Controller
Handles requests and connects Models with Views using Symfony Controllers.

This architecture ensures scalability, maintainability, and a clear separation of concerns.

---

## Contributors

Project developed by **Team One Health – Class 3A53**

- Emna Ben Aissa  
- Iheb Ben Jbir  
- Farah Hannachi  
- Emna Ben Bader  
- Mohamed Yassin Essaleh  

---

## Getting Started

### Prerequisites

- PHP (>= 8.x)
- Composer
- Symfony CLI
- MySQL
- Web Browser

### Installation

```bash
# Clone repository
git clone <repository-link>

# Enter project folder
cd curavita

# Install dependencies
composer install

# Configure environment variables
# Edit the .env file

# Create database
php bin/console doctrine:database:create

# Run migrations
php bin/console doctrine:migrations:migrate

# Start server
symfony server:start
```

---

## Acknowledgments

Thanks to our professors at **Esprit School of Engineering** for their guidance and support.

Inspired by modern e-health and online pharmacy platforms.
