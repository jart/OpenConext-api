package nl.surfnet.coin.api;

import nl.surfnet.coin.opensocial.service.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class PersonController {

    Logger LOG = LoggerFactory.getLogger(PersonController.class);

    @RequestMapping(value = "/social/people/{userId}/{groupId}")
    public String getPerson(@PathVariable("userId") String userId, @PathVariable("groupId") String groupId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Got getGroups-request, for userId '{}', groupId '{}'", userId, groupId);
        }
        return null;
    }
}
