# TaskGO UTM 🚀 (Version 2.1)

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 🗺️ Intelligent Real-Time Navigation
- **Map Selector**: Requesters can pin exact locations on a built-in map for precise task coordination.
- **GPS Integration**: Runners can navigate to task locations using external GPS apps (Google Maps, Apple Maps) directly from the task details.
- **Campus-Aware Centering**: The map automatically centers based on the selected campus (UTMKL/UTMJB).

### 💰 Mock Wallet System
- **In-App Earnings**: Runners now accumulate balance in a mock wallet upon successful task completion.
- **Transparent Balance**: Users can track their total earnings directly from their profile dashboard.

### 🤖 Advanced Agentic AI "Magic"
- **Magic Suggestion V2**: Intelligent form auto-fill that understands campus shorthand (JB/KL) and intent keywords (Food/Help).
- **Magic Completion Prompt**: Silent agent that monitors chat for completion intent and prompts the requester for verification.
- **Dispute Arbitrator**: AI-powered summaries of chat history for disputed tasks to assist Administrators.

### 🌓 Dynamic Appearance
- **Light/Dark Mode**: Full support for system-wide and manual theme switching.
- **UTM Branding**: Custom color schemes optimized for readability in both modes.

### 🛡️ Unified Admin Console (Enhanced in v2.1)
- **Evidence Investigation (TG-US24)**: View runner completion proofs and requester payment receipts alongside reported issues for full context.
- **Report Lifecycle Management (TG-US25)**: Interactive status updates for incident reports (Pending, Resolved, Action Taken).
- **Bad Review Monitoring (TG-US26)**: Dedicated dashboard to oversee and investigate low-quality service reviews (2-stars or below).
- **Embedded Console**: Admins can now use the app as a student while maintaining access to a dedicated "Console" tab.
- **Advanced Moderation**: Custom suspension logic (Days/Hours/Minutes) with real-time countdowns for restricted users.
- **Global Search & Filter**: Search and sort tasks, reports, and users with high-efficiency admin filters.

### 🎨 Creation Studio
- **Post & Offer**: Post requests or offer services with titles, descriptions, categories, and multiple image attachments.
- **Auto-Expanding Workspace**: Creation Studio sections now automatically expand if they contain active items.
- **Task Management**: Edit or cancel tasks with safety confirmations.

### 🛒 Dynamic Marketplace
- **Explore Tasks**: A unified feed with enhanced UI clarity and bold typography.
- **Advanced Filtering**: Filter by category, campus, and type (Request/Service).

### 🔄 Integrated Task Lifecycle
- **Proof-Based Completion**: Runner uploads proof -> Requester verifies -> Task completes.
- **Payment Verification**: Status tracking for PAID, PENDING, and DISPUTED states.

### 💬 Advanced Chat System
- **Real-time Messaging**: Contextual chats anchored to specific tasks.
- **Unread Tracking**: Badge indicators and notification routing.

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose.
- **Backend**: Firebase Authentication, Firestore, and Storage (Free Tier).
- **Maps**: Google Maps SDK for Android.
- **AI**: Google Gemini (Flash 1.5) with Heuristic Fallback.
- **Architecture**: MVVM with StateFlow & Shared ViewModel Store.

---

*Developed for the Project 2026 UTM community.*
