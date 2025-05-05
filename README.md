# Activity Tracker

A real-time user activity tracking system built with Spring Boot and React.

## Features

- Real-time activity tracking using WebSockets
- JWT-based authentication
- RESTful API for activity management
- H2 in memory database with Flyway migrations
- Redis caching for performance optimization
- React frontend with real-time updates

## Prerequisites

- Java 17 or higher
- Maven 3.8 or higher
- Redis 6 or higher
- Node.js 16 or higher
- npm 8 or higher

## Backend Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/activity-tracker.git
cd activity-tracker
```

2. Create a PostgreSQL database named `activity_tracker`

3. Update the database credentials in `src/main/resources/application.yml` if needed

4. Build and run the backend:
```bash
mvn clean install
mvn spring-boot:run
```

The backend will be available at `http://localhost:4000`

## API Endpoints

### Authentication
- POST `/api/auth/register` - Register a new user
- POST `/api/auth/login` - Login and get JWT token

### Activities
- POST `/api/activities` - Log a new activity
- GET `/api/activities` - Get paginated list of recent activities
- GET `/api/activities/user/{userId}` - Get activities by user ID
- GET `/api/activities/search` - Search activities with filters

### WebSocket
- Connect to `ws://localhost:8080/ws`
- Subscribe to `/topic/activities` for real-time updates

## Security

- JWT-based authentication
- Password encryption using BCrypt
- CORS configuration for frontend access
- Role-based access control

## Database Schema

The database schema is managed using Flyway migrations. The initial schema includes:

- `users` table for user management
- `activities` table for activity tracking

## Caching

Redis is used for caching:
- Recent activities
- User-specific activities
- Search results

## Testing

Run the test suite:
```bash
mvn test
```

## Assumptions

1. User IDs are stored as strings in the JWT token
2. Activities are immutable once created
3. Real-time updates are broadcast to all connected clients
4. Pagination is implemented for all list endpoints
5. Search functionality supports filtering by user ID and timestamp range

## Areas for Improvement

1. Add more comprehensive error handling
2. Implement rate limiting
3. Add more detailed activity types and categories
4. Implement activity aggregation and analytics
5. Add more sophisticated search capabilities
6. Implement activity archiving for old records
7. Add more comprehensive testing
8. Implement activity export functionality
9. Add more detailed user activity statistics
10. Implement activity templates for common actions 