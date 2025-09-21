=======
# Backend Service for FORIF_WEB

This project is a backend service built using **Java/Kotlin** with **Spring Boot**. It is deployed on AWS using various services such as EC2, RDS, SSM, S3, and Route53. The backend communicates with a **MySQL** database using **JPA** and provides a REST API for client applications.

## Technologies Used

- **Language**: Java
- **Framework**: Spring Boot
- **Database**: MySQL (accessed via JPA)
- **Cloud Services**:
    - AWS EC2 (Compute)
    - AWS RDS (Database)
    - AWS SSM (Parameter Store)
    - AWS S3 (Storage)
    - AWS Route53 (DNS Management)
- **API Documentation**: Swagger
- **Build Tool**: Gradle
- **CI/CD**: GitHub Actions
- **Logging**: slf4j

## Project Structure
- **src/main/java/forif/univ_hanyang**: Contains the Java/Kotlin source code.
- **src/main/resources**: Contains configuration files and static resources.

## Setting Up the Project

### Prerequisites

- Java 17 or higher
- Gradle
- AWS CLI configured with appropriate permissions
- MySQL database
- AWS RDS instance
- AWS S3 bucket
- AWS SSM parameter store
- AWS EC2 instance
- AWS Route53 domain
- GitHub repository

### Steps
- Clone the repository
- Set up the MySQL database
- Set up the AWS services
- Configure the application.yaml file
- Build the project
- Set up CI/CD
- Deploy the application
- Access the application
- Monitor the application
- Troubleshoot issues
- Update the application

## API Documentation
link: [Swagger](https://api.forif.org/swagger-ui/index.html#/)

## Contributors
- [Byeonghyun Yang](https://github.com/zxvm5962)
- Jihwan Yoon

## License
MIT License

Copyright (c) [2024] [Byeonghyun Yang]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Acknowledgements
- This project was inspired by the need for a backend service for the FORIF website.
- We would like to thank the FORIF members for their support and feedback.
- We would also like to thank the open-source community for providing tools and libraries that made this project possible.
- Finally, we would like to thank our university and friends for their encouragement and support.

## Contact Information
- Email(Club): forif.contact@gmail.com
- Email(Developer): zxvm5962@hanyang.ac.kr
- Website: [FORIF](https://forif.org)