# TaskGO UTM 🚀 (Version 2.2)

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 👤 Detailed Public Profiles (New in v2.2)
- **Universal Profile View**: Users and Admins can now view any student's detailed profile by clicking their name or avatar.
- **Activity Transparency**: Profiles display a user's posted tasks/services, completed work history as a runner, and their full review history from other community members.
- **Profile Reporting**: Integrated a reporting button directly on profiles to flag problematic users, which automatically increments their "Report Count" for administrative review.

### 🗺️ Intelligent Real-Time Navigation
- **Map Selector**: Requesters can pin exact locations on a built-in map for precise task coordination.
- **GPS Integration**: Runners can navigate to task locations using external GPS apps (Google Maps, Apple Maps) directly from the task details.
- **Campus-Aware Centering**: The map automatically centers based on the selected campus (UTMKL/UTMJB).

### 💰 Mock Wallet System
- **In-App Earnings**: Runners accumulate balance in a mock wallet upon successful task completion.
- **Transparent Balance**: Users can track their total earnings directly from their history dashboards.

### 🤖 Advanced Agentic AI "Magic"
- **Magic Suggestion V2**: Intelligent form auto-fill that understands campus shorthand (JB/KL) and intent keywords (Food/Help).
- **Smart Search**: AI-powered search filtering that processes natural language queries to apply category, campus, and task-type filters automatically.
- **Dispute Arbitrator**: AI-powered summaries of chat history for disputed tasks to assist Administrators.

### 🛡️ Unified Admin Console (Enhanced in v2.2)
- **Direct User Investigation**: Admins can now jump directly from the User Management list to a student's full public profile to review their entire history.
- **Evidence Investigation (TG-US24)**: View runner completion proofs and requester payment receipts alongside reported issues.
- **Bad Review Monitoring (TG-US26)**: Dedicated dashboard to oversee and investigate low-quality service reviews (2-stars or below) with direct profile access.
- **Embedded Console**: Admins can use the app as a student while maintaining access to a dedicated "Console" tab.
- **Advanced Moderation**: Custom suspension logic (Days/Hours/Minutes) for restricted users.

### 🎨 Creation Studio & My Tasks
- **Segmented History**: "My Posted Tasks" is now divided into **Requests** and **Services** tabs for better organization.
- **Terminological Accuracy**: The system now correctly distinguishes between "Interested Runners" (for requests) and "Interested Requesters" (for service offers).
- **Post & Offer**: Post items with titles, descriptions, categories, and multiple image attachments.

### 🛒 Dynamic Marketplace
- **Explore Tasks**: A unified feed with enhanced UI clarity, app branding, and improved layout to prevent price/title overlap.
- **Fixes**: Corrected alphabetical sorting (A-Z/Z-A) and optimized filtering performance.

### 🔄 Integrated Task Lifecycle
- **Proof-Based Completion**: Runner uploads proof -> Requester verifies -> Task completes.
- **Payment Verification**: Status tracking for PAID, PENDING, and DISPUTED states.

### 💬 Advanced Chat System
- **Real-time Messaging**: Contextual chats anchored to specific tasks.
- **Unread Tracking**: Badge indicators and notification routing.

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose.
- **Backend**: Firebase Authentication, Firestore, and Storage.
- **Maps**: Google Maps SDK for Android.
- **AI**: Google Gemini (Flash 1.5) with Heuristic Fallback.
- **Architecture**: MVVM with StateFlow & Shared ViewModel Store.

---

*Developed for the Project 2026 UTM community.*
