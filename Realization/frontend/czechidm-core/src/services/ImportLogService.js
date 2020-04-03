import AbstractService from './AbstractService';
import SearchParameters from '../domain/SearchParameters';

/**
 * Import logs
 *
 * @author Vít Švanda
 */
export default class ImportLogService extends AbstractService {

  getApiPath() {
    return '/import-logs';
  }

  getNiceLabel(entity) {
    if (!entity) {
      return '';
    }
    return `${entity.name}`;
  }

  /**
   * Agenda supports authorization policies
   */
  supportsAuthorization() {
    return true;
  }

  /**
   * Group permission - all base permissions (`READ`, `WRITE`, ...) will be evaluated under this group
   */
  getGroupPermission() {
    return 'EXPORTIMPORT';
  }

  /**
   * Almost all dtos doesn§t support rest `patch` method
   */
  supportsPatch() {
    return false;
  }

  /**
   * Returns default searchParameters for current entity type
   *
   * @return {object} searchParameters
   */
  getDefaultSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('created', 'desc');
  }

  /**
   * Returns search parameters for search roots
   */
  getRootSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK)
      .setFilter('roots', true)
      .clearSort()
      .setSort('type', 'asc')
      .setSize(25);
  }

  /**
   * Search children by parent id
   */
  getTreeSearchParameters() {
    return super.getDefaultSearchParameters().setName(SearchParameters.NAME_QUICK).clearSort().setSort('type', 'asc')
      .setSize(50);
  }
}
