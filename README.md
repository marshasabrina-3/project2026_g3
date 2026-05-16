# TaskGO UTM 🚀

TaskGO is a modern, student-centric marketplace application designed for the UTM community. It connects students who need assistance with those offering specialized services, fostering a helpful and efficient campus ecosystem.

## 🌟 Key Features

### 🎨 Creation Studio
- **Post & Offer**: Post requests or offer services with titles, descriptions, categories, and multiple image attachments.
- **Auto-Expanding Workspace**: Creation Studio sections (Requested Tasks, Service Offers, Applications) now automatically expand if they contain active items, ensuring immediate focus on your current tasks.
- **Task Management**:
    - **Edit Active Tasks**: Re-navigate to the posting page to update details for any "Open" task.
    - **Cancel with Confirmation**: Safely cancel tasks with a confirmation dialog; status updates to "Cancelled" in the database.
- **Categorized Activity**: Separate lists for Requests, Services, and Applications Sent.
- **Flexible Options**: Opt to include or omit specific deadlines and prices for service offers.
- **Attachment Support**: Upload up to 7 images per post for visual context.

### 🛒 Dynamic Marketplace
- **Explore Tasks**: A unified feed of "Open Requests" and "Service Offers".
- **Enhanced UI Clarity**: Redesigned filter dropdowns with high-contrast labels and bold typography for superior readability in the "Explore" tab.
- **Advanced Filtering**: Filter by category (Food, Carpool, Printing, etc.) and campus (UTMKL or UTMJB).
- **Smart Sorting**: Sort tasks by Newest First, Highest/Lowest Price, or Alphabetically.
- **Detailed View**: High-resolution image carousel, detailed descriptions, timestamps (Posted/Completed), and direct access to poster profiles.

### 🔄 Integrated Task Lifecycle
- **Proof-Based Completion**:
    - **Runner**: Marks task as finished by uploading a **Completion Proof** image.
    - **Requester**: Verifies the proof on the detailed page before clicking **Confirm Completion**.
- **Payment Verification**:
    - **Status Tracking**: Manage payment states: **Pending**, **Paid**, or **Disputed**.
    - **Chat-Integrated Proofs**: Send payment receipts directly via a specialized shortcut in the chat.
    - **Visual Badges**: Payment proofs are highlighted with "PAYMENT PROOF" badges in the conversation and linked to the task record.

### 💬 Advanced Chat System
- **Real-time Messaging**: Instant communication powered by Firebase Firestore.
- **Task-Contextual**: Chats are anchored to tasks with reference headers.
- **Unread Tracking**: Badge indicators for unread messages.
- **Payment Shortcut**: Specialized button for requesters to send transaction proofs instantly.

### 🛡️ User Reputation & Safety
- **Enhanced Profiles**: View user names, average star ratings, and a scrollable list of the latest reviews.
- **Dynamic Rating System**: Requesters are prompted to rate and review runners immediately after task confirmation.
- **Reporting System**: 
    - **User/Task Reporting**: Report fraudulent activity or unprofessional behavior with categorized reasons.
    - **App Issue Reporting**: Dedicated page to report bugs, UI issues, or safety concerns directly to the dev team.

## 🛠️ Technical Architecture

- **UI Framework**: Jetpack Compose.
- **Backend**: Firebase Authentication & Firestore.
- **Storage**: Optimized Base64 image system for quick rendering without external storage dependencies.
- **Architecture**: MVVM with StateFlow for real-time UI updates.

## 🎨 Design Language
Utilizes **UTM Maroon (#800000)** for branding, combined with Material 3 components and a clean, responsive layout.

---

*Developed for the Project 2024 UTM community.*
