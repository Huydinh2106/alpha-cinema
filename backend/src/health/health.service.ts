import { Injectable } from '@nestjs/common';

@Injectable()
export class HealthService {
  getHealth() {
    return {
      status: 'ok',
      service: 'alpha-cinema-backend',
      timestamp: new Date().toISOString(),
    };
  }
}
