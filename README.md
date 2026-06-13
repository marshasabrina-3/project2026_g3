# TaskGO UTM 🚀 (Version 2.4)

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 🛡️ Enhanced Trust & Safety (New in v2.4)
- **Advanced User Reporting**: Integrated a categorized reporting system (Scam, Bullying, Fraud, etc.) directly on user profiles. Reporting a user automatically increments their report count and generates a detailed audit log for admins formatted as `[Category]: "Details"`.
- **Account Status Transparency**: Public profiles now clearly display account statuses (ACTIVE, SUSPENDED, or BANNED), ensuring community awareness of user standing.
- **Robust Admin Navigation**: Improved navigation within the Admin Console to preserve tab state when investigating profiles.
- **Action Security**: Restricted "Report Runner" and "Write Review" actions on public profiles to ensure only the original requester can perform these actions from their own history page.

### 📱 Responsive & Adaptive UI
- **Marketplace Clarity**: High-clarity unified feed with improved item layout to prevent price and title overlap.
- **Tablet Optimization**: Enhanced the main navigation bar with adaptive height and system padding to ensure a consistent experience on larger devices.
- **Brand Identity**: Updated app logo integration on the home page for a cleaner, modern look.

### 👤 Detailed Public Profiles
- **Universal Profile View**: Users and Admins can view any student's detailed profile with activity history.
- **Activity Transparency**: Profiles display posted tasks/services and completed work history as a runner, including payment amounts and requester attribution.

### 🗺️ Intelligent Real-Time Navigation
- **Map Selector**: Pin exact locations on a built-in map for precise coordination.
- **GPS Integration**: Navigate using external GPS apps directly from task details.
- **Campus-Aware Centering**: Automatically centers based on selected campus (UTMKL/UTMJB).

### 🤖 Advanced Agentic AI "Magic"
- **Magic Suggestion V2**: Intelligent form auto-fill that understands campus shorthand and intent.
- **Smart Search**: Natural language processing for instant filtering.
- **Dispute Arbitrator**: AI-generated summaries of disputes to assist Administrators.

### 🛡️ Unified Admin Console
- **Direct User Investigation**: Jump from management lists to full public profiles.
- **Evidence Investigation**: Match runner completion proofs with requester payment receipts.
- **Bad Review Monitoring**: Dedicated dashboard for low-quality service reviews.

### 🛒 Dynamic Marketplace
- **Optimized Sorting**: Fully corrected alphabetical (A-Z/Z-A) and price-based sorting logic.

### 🔄 Integrated Task Lifecycle
- **Proof-Based Completion**: Integrated verification workflow for both runners and requesters.
- **Payment Verification**: Real-time status tracking (PAID, PENDING, DISPUTED).

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose (Adaptive Navigation).
- **Backend**: Firebase Authentication, Firestore, and Storage.
- **Maps**: Google Maps SDK for Android.
- **AI**: Google Gemini (Flash 1.5).
- **Architecture**: MVVM with StateFlow & Persistent Navigation State.

---

*Developed for the Project 2026 UTM community.*
