<?php

namespace App\Service;

use App\Entity\Question;

class TicketPriorityAssigner
{
    public function __construct(private GroqLlmService $llm) {}

    public function assignPriority(Question $question): string
    {
        // Build the text you want the LLM to judge
        $title = method_exists($question, 'getObjet') ? (string) $question->getObjet() : '';
        $details = method_exists($question, 'getDescription') ? (string) $question->getDescription() : '';
        $type = method_exists($question, 'getTypeTicket') ? (string) $question->getTypeTicket() : '';

        $system = <<<SYS
You are a support triage assistant.
Classify ticket priority into exactly one of:
- basse (low)
- normale (medium)
- haute (high)

Rules:
- haute: security risk, data loss, payment blocking, system down, urgent deadline today, legal/critical complaint, production outage
- normale: standard bug, feature not working with workaround, moderate impact
- basse: question/info request, cosmetic issue, minor inconvenience

Return ONLY valid JSON in this exact format:
{"priorite":"basse|normale|haute"}
No extra keys, no explanations.
SYS;

        $user = <<<USR
Ticket data:
- type: {$type}
- title: {$title}
- details: {$details}
USR;

        $result = $this->llm->chat([
            ['role' => 'system', 'content' => $system],
            ['role' => 'user', 'content' => $user],
        ], temperature: 0.0, maxTokens: 80);

        $content = $result['choices'][0]['message']['content'] ?? '';
        $priority = $this->extractPriorityFromJson($content);

        // Safety fallback if model fails
        if (!in_array($priority, ['basse', 'normale', 'haute'], true)) {
            return 'normale';
        }

        return $priority;
    }

    private function extractPriorityFromJson(string $content): ?string
    {
        // Try direct JSON decode
        $decoded = json_decode(trim($content), true);
        if (is_array($decoded) && isset($decoded['priorite'])) {
            return is_string($decoded['priorite']) ? $decoded['priorite'] : null;
        }

        // If model adds junk, try to find the first {...} block
        if (preg_match('/\{.*\}/s', $content, $m)) {
            $decoded = json_decode($m[0], true);
            if (is_array($decoded) && isset($decoded['priorite']) && is_string($decoded['priorite'])) {
                return $decoded['priorite'];
            }
        }

        return null;
    }
}