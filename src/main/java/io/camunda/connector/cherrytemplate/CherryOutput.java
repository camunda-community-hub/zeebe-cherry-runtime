package io.camunda.connector.cherrytemplate;

import java.util.List;
import java.util.Map;

public interface CherryOutput {
  /**
   * get the list of Output Parameters
   *
   * @return list of Map. Map contains key "name", "label", "default", "class", "level", "explanation"
   */
  List<Map<String, Object>> getOutputParameters();

}
