# 🤖 AI-Powered Career Opportunity Distribution System

**Jobify CVUT** is an intelligent Discord bot that connects students of FIT ČVUT with relevant internship and job opportunities. It uses the EXPERTS.AI platform for opportunity sourcing, OpenAI's GPT for CV analysis and recommendations, and PostgreSQL for persistent profile and opportunity tracking.

---

## 📌 Features

- 🎯 **Personalized Job Matching** – Based on skills and career interests stored in student profiles.
- 📄 **CV Upload & Parsing** – Automatically extracts name, email, skills, and positions from uploaded PDF resumes.
- 🧠 **GPT-Powered Career Advisor** – Analyzes saved opportunities and CVs to suggest improvements or ideal positions.
- 👤 **Interactive Profile Setup** – Discord-driven onboarding via buttons and dropdowns.
- 🔄 **Opportunity Synchronization** – Periodic and real-time fetching from the EXPERTS.AI API.
- 💾 **Persistent Data Layer** – PostgreSQL-backed storage of students and their assigned opportunities.
- 📝 **Feedback Logging** – Stores GPT-based resume suggestions and ratings using `FeedbackDAO`.

---

## 📁 Project Structure

```
src/
├── main/
│   └── java/
│       └── bot/
│           ├── BotMain.java               # Entry point
│           ├── CommandHandler.java        # Handles messages and commands
│           ├── InteractionHandler.java    # Handles buttons and dropdowns
│           ├── GPTClient.java             # GPT API handler
│           ├── api/
│           │   └── OpportunityClient.java # EXPERTS.AI integration
│           ├── storage/
│           │   ├── StudentDAO.java        # DB access for students
│           │   ├── OpportunityDAO.java    # DB access for opportunities
│           │   └── FeedbackDAO.java       # Stores GPT feedback and ratings
│           └── util/
│               └── PdfUtils.java          # Resume text extraction

```

---

## ⚙️ Installation & Setup

### 1. Requirements

- Java 17+
- PostgreSQL
- Discord Bot Token
- OpenAI API Key (optional)

### 2. Environment Variables

Set the following variables:

```env
DISCORD_TOKEN=your_discord_bot_token
OPENAI_API_KEY=your_openai_key
```

### 3. Database Schema

```sql
CREATE TABLE student (
  id SERIAL PRIMARY KEY,
  name TEXT,
  email TEXT,
  skills TEXT,
  career_interest TEXT,
  discord_id TEXT UNIQUE,
  cv_text TEXT
);

CREATE TABLE opportunities (
  opportunity_id TEXT,
  discord_id TEXT,
  title TEXT,
  description TEXT,
  job_type TEXT,
  application_deadline DATE,
  url TEXT,
  wage TEXT,
  home_office TEXT,
  benefits TEXT,
  formal_requirements TEXT,
  technical_requirements TEXT,
  contact_person TEXT,
  company TEXT,
  PRIMARY KEY (opportunity_id, discord_id)
);

CREATE TABLE feedback (
  id SERIAL PRIMARY KEY,
  feedback_text TEXT,
  stars INTEGER,
  discord_id TEXT
);
```

### 4. Run the Bot

```bash
./gradlew run
# or with plain Java
java -cp your-jar-name.jar bot.BotMain
```

---

## 🚀 Usage

### Commands

| Command       | Description                              |
|---------------|------------------------------------------|
| `!start`      | Begins onboarding with buttons           |
| `!ask <text>` | Asks GPT for personalized guidance       |
| `!fetch`      | Manually fetches job matches             |
| `!status`     | Bot status check                         |

### Interaction Flow

1. User types `!start`
2. Bot responds in DM with options to:
   - Upload CV
   - Create profile manually
   - Ask GPT questions
   - Match jobs
3. User receives GPT-based analysis or matched jobs

---

## 💡 Example GPT Prompts

```text
!ask Based on my profile and the jobs below, which one should I apply to?
!ask How can I improve my CV for DevOps positions?
!ask Which technical skills should I focus on for backend roles?
```

---

## 📌 Roadmap

- 🔔 Notification system for new job opportunities
- 🌍 Location-based filtering
- 🗃️ Admin panel for company management
- 🌐 Multilingual support (EN/CZ)

---

## 🙋‍♂️ Support

For help or questions, please contact:

- Email: francisco.molina.antonio@gmail.com

- Email: ErdemYusufEmre@gmail.com

- Email: emreyuce228@gmail.com

- GitLab Issues: [Open a ticket](https://gitlab.fit.cvut.cz/molinfr1/ai-powered-career-opportunity-distribution-system/-/issues)

---

## 👨‍💻 Authors and Acknowledgment

This project was developed by a collaborative team of students from FIT ČVUT. Below are the contributors and their specific roles:

- **Francisco Antonio Molina Alava** – 🧠 Team Leader & Main Developer    
  Led the team, directed the architecture and main code development, improved the database schema, and coordinated the documentation process.

- **Yunus Emre Yuce** – 🗂️ Documentation & Initial Database  
  Contributed significantly to the documentation and built the first version of the database.

- **Yusuf Emre Erdem** – 💻 Developer & Documentation  
  Participated in writing documentation, contributed to backend logic, and assisted in development tasks.

- **Emir Orhan** – 🗂️ Documentation & Initial Database  
  Helped with the documentation and collaborated on the initial database setup and validation.

- **Abdul Rahman Asaad Mourad** – 📄 Documentation & Code Quality  
  Contributed to the documentation and performed static code analysis to ensure code quality and maintainability.

- **Karim Gamal Aziz Georgy Habib** – 📄 Documentation  
  Assisted with creating and formatting the official project documentation.

- **Maya Hussein Abdulhalem Elkadi** – 📄 Documentation  
  Supported the documentation effort across various project components.

We thank all contributors for their collaboration and effort in building this AI-powered opportunity matching system.


## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

---

## 📊 Project Status

🚧 Actively maintained. New features are being developed regularly.  
Looking for collaborators and testers!